# frozen_string_literal: true

require 'minitest/autorun'
require 'fileutils'
require 'json'
require 'open3'
require 'tmpdir'

class DeployTest < Minitest::Test
  SCRIPTS = File.expand_path('..', __dir__)
  TAG = 'a' * 40
  APP_DIGEST = "sha256:#{'b' * 64}"
  FRONTEND_DIGEST = "sha256:#{'c' * 64}"

  def setup
    @dir = Dir.mktmpdir('happygallery-deploy-')
    @scripts = File.join(@dir, 'deploy', 'k3s', 'scripts')
    @bin = File.join(@dir, 'bin')
    FileUtils.mkdir_p([@scripts, @bin])
    %w[common.sh deploy.sh update-release-images.rb].each do |name|
      FileUtils.cp(File.join(SCRIPTS, name), @scripts)
    end
    @release = File.join(@dir, 'release.env')
    @original = "# 운영 설정\nPUBLIC_HOST=happy-gallery.com\nIMAGE_TAG=old\nACME_EMAIL=ops@example.com\nVERIFIED_RECOVERY_BUNDLE=/backup/file.recovery.env\nCUSTOM_VALUE=a=b\n"
    File.write(@release, @original)
    File.chmod(0o600, @release)
    @calls = File.join(@dir, 'calls.jsonl')
    @images = File.join(@dir, 'images.json')
    images = {}
    { 'app' => APP_DIGEST, 'frontend' => FRONTEND_DIGEST }.each do |name, digest|
      image = "localhost/happygallery-#{name}:#{TAG}"
      [image, "#{image}@#{digest}", "localhost/happygallery-#{name}@#{digest}"].each { |ref| images[ref] = digest }
    end
    File.write(@images, JSON.generate(images))
    @env = { 'PATH' => "#{@bin}:#{ENV.fetch('PATH')}",
             'K3S_BIN' => File.join(@bin, 'k3s'), 'SYSTEMCTL_BIN' => File.join(@bin, 'systemctl'),
             'HAPPYGALLERY_RELEASE_DIR' => File.join(@dir, 'releases'),
             'DEPLOY_TEST_CALLS' => @calls, 'DEPLOY_TEST_IMAGES' => @images,
             'DEPLOY_TEST_TIMER' => 'active', 'DEPLOY_TEST_STATE' => 'inactive',
             'DEPLOY_TEST_BUILD_EXIT' => '0', 'DEPLOY_TEST_ROLLOUT_EXIT' => '0' }
    executable(File.join(@bin, 'git'), "puts '#{TAG}'")
    # flock은 Linux 실행 경계다. 파일 내용·원자 교체·Ruby 잠금은 실제로 검사한다.
    executable(File.join(@bin, 'flock'), 'exit 0')
    executable(File.join(@bin, 'sudo'), "ARGV.shift if ARGV.first == '--'; exec(*ARGV)")
    executable(File.join(@bin, 'sleep'), 'exit 0')
    executable(File.join(@bin, 'k3s'), <<~'RUBY')
      require 'json'
      abort 'unexpected containerd operation' unless ARGV == %w[ctr images list]
      puts 'REF TYPE DIGEST SIZE PLATFORMS LABELS'
      JSON.parse(File.read(ENV.fetch('DEPLOY_TEST_IMAGES'))).each do |name, digest|
        puts "#{name} manifest #{digest} 1MiB linux/amd64 -"
      end
    RUBY
    executable(File.join(@bin, 'systemctl'), <<~'RUBY')
      require 'json'
      File.open(ENV.fetch('DEPLOY_TEST_CALLS'), 'a') { |f| f.puts JSON.generate(['systemctl', *ARGV]) }
      case ARGV.first
      when 'is-active' then exit(ENV.fetch('DEPLOY_TEST_TIMER') == 'active' ? 0 : 3)
      when 'show'
        states = ENV.fetch('DEPLOY_TEST_STATE').split(',')
        count = File.readlines(ENV.fetch('DEPLOY_TEST_CALLS')).count { |line| JSON.parse(line)[1] == 'show' }
        puts states.fetch(count - 1, states.last)
      when 'start', 'stop' then exit 0
      else abort 'unexpected systemctl call'
      end
    RUBY
    %w[build-import-images.sh rollout.sh].each do |name|
      phase = name.start_with?('build') ? 'BUILD' : 'ROLLOUT'
      executable(File.join(@scripts, name), <<~RUBY)
        require 'json'
        File.open(ENV.fetch('DEPLOY_TEST_CALLS'), 'a') { |f| f.puts JSON.generate(['#{phase}', *ARGV]) }
        exit Integer(ENV.fetch('DEPLOY_TEST_#{phase}_EXIT'))
      RUBY
    end
  end

  def teardown
    FileUtils.remove_entry(@dir)
  end

  def executable(path, body)
    File.write(path, "#!/usr/bin/env ruby\n#{body}\n")
    File.chmod(0o700, path)
  end

  def deploy(imported: false)
    args = imported ? ['--imported', @release] : [@release]
    Open3.capture3(@env, 'bash', File.join(@scripts, 'deploy.sh'), *args)
  end

  def update(*values)
    Open3.capture3('ruby', File.join(@scripts, 'update-release-images.rb'), @release, *values)
  end

  def calls
    File.exist?(@calls) ? File.readlines(@calls).map { |line| JSON.parse(line) } : []
  end

  def test_build_import_updates_only_image_values_and_deploys_after_backup_finishes
    @env['DEPLOY_TEST_STATE'] = 'activating,inactive'
    output, error, status = deploy
    assert status.success?, error
    contents = File.read(@release)
    @original.lines.reject { |line| line.start_with?('IMAGE_TAG=') }.each { |line| assert_includes contents, line }
    assert_includes contents, "IMAGE_TAG=#{TAG}\n"
    assert_includes contents, "APP_IMAGE=localhost/happygallery-app:#{TAG}\n"
    assert_includes contents, "FRONTEND_IMAGE_DIGEST=#{FRONTEND_DIGEST}\n"
    assert_equal @original, File.read("#{@release}.previous")
    assert_equal 0o600, File.stat(@release).mode & 0o777
    assert_equal 0o600, File.stat("#{@release}.previous").mode & 0o777
    phases = calls.map { |call| call[0] == 'systemctl' ? call[1] : call[0] }
    assert_equal %w[BUILD is-active stop show show ROLLOUT start], phases
    assert_includes output, '자동 배포 완료'
  end

  def test_previously_imported_images_skip_build_and_keep_disabled_timer_disabled
    @env['DEPLOY_TEST_TIMER'] = 'inactive'
    _output, error, status = deploy(imported: true)
    assert status.success?, error
    refute calls.any? { |call| call[0] == 'BUILD' || %w[start stop].include?(call[1]) }
    assert calls.any? { |call| call[0] == 'ROLLOUT' }
  end

  def test_build_failure_preserves_config_and_does_not_touch_timer_or_rollout
    @env['DEPLOY_TEST_BUILD_EXIT'] = '23'
    _output, _error, status = deploy
    assert_equal 23, status.exitstatus
    assert_equal @original, File.read(@release)
    refute File.exist?("#{@release}.previous")
    assert_equal ['BUILD'], calls.map(&:first)
  end

  def test_conflicting_or_missing_imported_alias_does_not_update_config
    [nil, FRONTEND_DIGEST].each do |bad_digest|
      images = JSON.parse(File.read(@images))
      ref = "localhost/happygallery-app@#{APP_DIGEST}"
      bad_digest ? images[ref] = bad_digest : images.delete(ref)
      File.write(@images, JSON.generate(images))
      _output, _error, status = deploy(imported: true)
      refute status.success?
      assert_equal @original, File.read(@release)
      assert_empty calls
    end
  end

  def test_failed_rollout_keeps_previous_config_and_pauses_timer_for_recovery
    @env['DEPLOY_TEST_ROLLOUT_EXIT'] = '24'
    output, _error, status = deploy(imported: true)
    assert_equal 24, status.exitstatus
    assert_equal @original, File.read("#{@release}.previous")
    assert_includes File.read(@release), "IMAGE_TAG=#{TAG}\n"
    refute calls.any? { |call| call[1] == 'start' }
    assert_includes output, '백업 예약을 중지 상태로 유지합니다'
  end

  def test_pre_deploy_failure_restores_previously_active_timer
    @env['DEPLOY_TEST_STATE'] = 'unknown'
    _output, _error, status = deploy(imported: true)
    refute status.success?
    assert_equal @original, File.read(@release)
    assert calls.any? { |call| call[1] == 'start' }
    refute calls.any? { |call| call[0] == 'ROLLOUT' }
  end

  def test_invalid_or_duplicate_values_are_rejected_without_replacing_config
    [[TAG, 'bad', FRONTEND_DIGEST], ['latest', APP_DIGEST, FRONTEND_DIGEST]].each do |values|
      _output, _error, status = update(*values)
      refute status.success?
      assert_equal @original, File.read(@release)
    end
    File.write(@release, @original + "IMAGE_TAG=duplicate\n")
    _output, error, status = update(TAG, APP_DIGEST, FRONTEND_DIGEST)
    refute status.success?
    assert_includes error, '중복된 환경 변수: IMAGE_TAG'
    assert_equal @original + "IMAGE_TAG=duplicate\n", File.read(@release)
    refute File.exist?("#{@release}.previous")
  end

  def test_repeated_update_preserves_previous_config_and_refuses_concurrent_writer
    2.times do
      _output, error, status = update(TAG, APP_DIGEST, FRONTEND_DIGEST)
      assert status.success?, error
    end
    assert_equal @original, File.read("#{@release}.previous")
    File.open("#{@release}.lock", 'w') do |lock|
      lock.flock(File::LOCK_EX)
      _output, error, status = update(TAG, APP_DIGEST, FRONTEND_DIGEST)
      refute status.success?
      assert_includes error, '다른 release.env 갱신이 진행 중'
    end
  end
end
