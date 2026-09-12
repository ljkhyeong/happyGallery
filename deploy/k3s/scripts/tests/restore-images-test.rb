# frozen_string_literal: true

require 'minitest/autorun'
require 'digest'
require 'fileutils'
require 'json'
require 'open3'
require 'tmpdir'
require 'yaml'

class RestoreImagesTest < Minitest::Test
  SCRIPT = File.expand_path('../prepare-restored-release-images.sh', __dir__)
  TAG = 'a' * 40
  APP_DIGEST = "sha256:#{'a' * 64}"
  FRONTEND_DIGEST = "sha256:#{'b' * 64}"
  RUNTIME_DIGEST = "sha256:#{'c' * 64}"
  APP = "localhost/happygallery-app:#{TAG}@#{APP_DIGEST}"
  FRONTEND = "localhost/happygallery-frontend:#{TAG}@#{FRONTEND_DIGEST}"
  APP_CRI = "localhost/happygallery-app@#{APP_DIGEST}"
  FRONTEND_CRI = "localhost/happygallery-frontend@#{FRONTEND_DIGEST}"
  TARGETS = { 'MYSQL' => ['StatefulSet', 'mysql'], 'REDIS' => ['Deployment', 'redis'],
              'PROMETHEUS' => ['Deployment', 'prometheus'],
              'ALERTMANAGER' => ['Deployment', 'alertmanager'],
              'GRAFANA' => ['Deployment', 'grafana'] }.freeze

  def setup
    @directory = Dir.mktmpdir('happygallery-restore-images-')
    @release = File.join(@directory, 'release')
    FileUtils.mkdir_p(@release)
    @state = File.join(@directory, 'state.json')
    @calls = File.join(@directory, 'calls.jsonl')
    @k3s = File.join(@directory, 'k3s')
    File.write(@state, '{}')
    File.write(@k3s, <<~'RUBY')
      #!/usr/bin/env ruby
      require 'json'
      File.open(ENV.fetch('IMAGE_TEST_CALLS'), 'a') { |file| file.puts JSON.generate(ARGV) }
      abort 'unexpected executable' unless ARGV.shift == 'ctr'
      state_file = ENV.fetch('IMAGE_TEST_STATE')
      state = JSON.parse(File.read(state_file))
      case ARGV.shift(2)
      when ['images', 'list']
        if ARGV == ['-q']
          puts state.keys
        else
          puts 'REF TYPE DIGEST SIZE PLATFORMS LABELS'
          state.each { |name, digest| puts "#{name} manifest #{digest} 1MiB linux/amd64 -" }
        end
      when ['images', 'import']
        state.merge!(JSON.parse(File.read(ARGV.fetch(0))))
      when ['images', 'tag']
        source, target = ARGV
        abort 'existing image must not be overwritten' if state.key?(target)
        state[target] = state.fetch(source)
      else
        abort 'unexpected containerd operation'
      end
      File.write(state_file, JSON.generate(state))
    RUBY
    File.chmod(0o755, @k3s)
    write_bundle_file('metadata.env', <<~ENV)
      IMAGE_TAG=#{TAG}
      APP_IMAGE=localhost/happygallery-app:#{TAG}
      FRONTEND_IMAGE=localhost/happygallery-frontend:#{TAG}
      APP_IMAGE_DIGEST=#{APP_DIGEST}
      FRONTEND_IMAGE_DIGEST=#{FRONTEND_DIGEST}
    ENV
    @archive_images = { APP => APP_DIGEST, FRONTEND => FRONTEND_DIGEST }
    runtime_metadata = []
    manifests = TARGETS.map do |key, (kind, name)|
      image = "docker.io/example/#{name}:1"
      @archive_images[image] = RUNTIME_DIGEST
      runtime_metadata.concat(["#{key}_IMAGE=#{image}", "#{key}_IMAGE_DIGEST=#{RUNTIME_DIGEST}"])
      { 'kind' => kind, 'metadata' => { 'name' => name },
        'spec' => { 'template' => { 'spec' => { 'containers' => [{ 'name' => name, 'image' => image }] } } } }
    end
    write_bundle_file('manifests.yaml', manifests.map(&:to_yaml).join)
    write_bundle_file('runtime-images.env', runtime_metadata.join("\n") + "\n")
    # 실제 OCI 형식 대신 명령 경계의 이미지 목록을 모사한다.
    write_bundle_file('images.tar', JSON.generate(@archive_images))
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def write_bundle_file(name, contents)
    path = File.join(@release, name)
    File.write(path, contents)
    File.write("#{path}.sha256", "#{Digest::SHA256.hexdigest(contents)}  #{name}\n")
  end

  def run_restore
    Open3.capture3({ 'K3S_BIN' => @k3s, 'IMAGE_TEST_STATE' => @state,
                    'IMAGE_TEST_CALLS' => @calls }, 'bash', SCRIPT, @release)
  end

  def calls
    File.readlines(@calls).map { |line| JSON.parse(line) }
  end

  def test_empty_store_imports_legacy_archive_and_registers_cri_names
    output, error, status = run_restore
    assert status.success?, error
    restored = JSON.parse(File.read(@state))
    assert_equal APP_DIGEST, restored.fetch(APP_CRI)
    assert_equal FRONTEND_DIGEST, restored.fetch(FRONTEND_CRI)
    assert_equal @archive_images, restored.reject { |name, _| [APP_CRI, FRONTEND_CRI].include?(name) }
    assert_equal 1, calls.count { |call| call[1..2] == ['images', 'import'] }
    assert_includes output, '모든 이미지 digest를 확인했습니다'
  end

  def test_existing_legacy_images_get_aliases_without_reimport_and_can_be_retried
    File.write(@state, JSON.generate(@archive_images))
    2.times do
      _output, error, status = run_restore
      assert status.success?, error
    end
    refute calls.any? { |call| call[1..2] == ['images', 'import'] }
    assert_equal 2, calls.count { |call| call[1..2] == ['images', 'tag'] }
  end

  def test_existing_conflicting_aliases_are_preserved_before_any_import
    [APP, FRONTEND, APP_CRI, FRONTEND_CRI].each do |reference|
      initial = { reference => RUNTIME_DIGEST }
      File.write(@state, JSON.generate(initial))
      _output, error, status = run_restore
      refute status.success?, reference
      assert_includes error, '기존 복구 이미지 별칭의 digest가 다릅니다'
      assert_equal initial, JSON.parse(File.read(@state))
    end
    refute calls.any? { |call| %w[import tag].include?(call[2]) }
  end

  def test_imported_digest_mismatch_is_rejected_for_app_and_runtime
    [APP, 'docker.io/example/mysql:1'].each do |reference|
      File.write(@state, '{}')
      write_bundle_file('images.tar', JSON.generate(@archive_images.merge(reference => FRONTEND_DIGEST)))
      _output, error, status = run_restore
      refute status.success?, reference
      assert_includes error, 'import 후 필수 app/frontend/runtime 이미지 digest'
      refute JSON.parse(File.read(@state)).key?(APP_CRI)
    end
  end

  def test_archive_missing_frontend_cannot_report_success
    write_bundle_file('images.tar', JSON.generate(@archive_images.reject { |name, _| name == FRONTEND }))
    _output, error, status = run_restore
    refute status.success?
    assert_includes error, 'import 후 필수 app/frontend/runtime 이미지 digest'
    refute JSON.parse(File.read(@state)).key?(APP_CRI)
  end
end
