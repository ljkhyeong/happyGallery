# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'socket'
require_relative '../gitops-control'
require_relative '../publish-gitops'

class GitopsTest < Minitest::Test
  def setup
    @directory = Dir.mktmpdir('happygallery-gitops-test-')
    @release = { 'source_sha' => 'a' * 40, 'app_digest' => "sha256:#{'b' * 64}",
                 'frontend_digest' => "sha256:#{'c' * 64}", 'acme_email' => 'ops@example.com',
                 'manifest_sha256' => 'd' * 64 }
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def test_application_and_project_manifest_parse
    path = File.expand_path('../../argocd/application.yaml', __dir__)
    documents = YAML.load_stream(File.read(path))
    assert_equal %w[AppProject Application], documents.map { |document| document.fetch('kind') }
    assert_equal documents.first.dig('metadata', 'name'), documents.last.dig('spec', 'project')
  end

  def test_control_rejects_shell_input_unknown_fields_and_unknown_phases
    %w[source_sha app_digest frontend_digest manifest_sha256 acme_email].each do |key|
      assert_raises(RuntimeError) do
        GitopsControl.request(JSON.generate(@release.merge('phase' => 'prepare', key => "value\ncommand")))
      end
    end
    assert_raises(RuntimeError) { GitopsControl.request(JSON.generate(@release.merge('phase' => 'delete'))) }
    assert_raises(RuntimeError) { GitopsControl.request(JSON.generate(@release.merge('phase' => 'prepare', 'command' => 'id'))) }
    assert_equal ['prepare', @release], GitopsControl.request(JSON.generate(@release.merge('phase' => 'prepare')))
  end

  def test_hook_transports_json_over_unix_socket_and_propagates_host_failure
    receiver = nil
    socket_directory = Dir.mktmpdir('hg-gitops-', '/tmp')
    socket_path = File.join(socket_directory, 'control.sock')
    server = UNIXServer.new(socket_path)
    hook = GitopsRelease.hook('prepare', @release)
    container = hook.dig('spec', 'template', 'spec', 'containers', 0)
    script = container.fetch('command').last.sub('/control/control.sock', socket_path)
    environment = { 'GITOPS_REQUEST' => container.dig('env', 0, 'value') }
    [true, false].each do |ok|
      receiver = Thread.new do
        client = server.accept
        request = JSON.parse(client.gets)
        client.write(JSON.generate('ok' => ok, 'message' => '호스트 검사 결과'))
        client.close
        request
      end
      output, error, status = Timeout.timeout(15) { Open3.capture3(environment, 'node', '-e', script) }
      assert_equal ok, status.success?, error
      assert_includes output, '호스트 검사 결과'
      assert_equal @release.merge('phase' => 'prepare'), receiver.value
    end
  ensure
    receiver&.kill
    server&.close
    FileUtils.remove_entry(socket_directory) if socket_directory
  end

  def test_host_control_reuses_pending_request_and_rejects_a_different_release
    previous_home = ENV['HOME']
    ENV['HOME'] = @directory
    state = File.join(@directory, '.local/state/happygallery')
    checkout = File.join(state, 'cd/gitops-source')
    scripts = File.join(checkout, 'deploy/k3s/scripts')
    FileUtils.mkdir_p(scripts)
    File.write(File.join(scripts, 'deploy-registry-images.sh'), <<~'SH')
      set -eu
      mkdir -p "$HAPPYGALLERY_RELEASE_DIR"
      echo candidate > "$HAPPYGALLERY_RELEASE_DIR/gitops-active"
    SH
    File.write(File.join(scripts, 'rollout.sh'), <<~'SH')
      set -eu
      echo "$2" >> "$HAPPYGALLERY_RELEASE_DIR/phases"
      if [ "$2" = complete ]; then rm "$HAPPYGALLERY_RELEASE_DIR/gitops-active"; fi
    SH
    commands = []
    git = lambda do |*arguments|
      commands << arguments
      case arguments.first
      when 'remote' then 'https://github.com/ljkhyeong/happyGallery.git'
      when 'show' then JSON.generate(@release)
      when '-C' then arguments.include?('rev-parse') ? @release.fetch('source_sha') : ''
      else ''
      end
    end
    GitopsControl.stub(:git, git) do
      GitopsControl.handle('prepare', @release)
      initial_commands = commands.length
      GitopsControl.handle('prepare', @release)
      refute commands[initial_commands..].any? { |arguments| arguments.first == 'fetch' }
      assert_raises(RuntimeError) { GitopsControl.handle('prepare', @release.merge('source_sha' => 'e' * 40)) }
      GitopsControl.handle('failed', @release)
      assert File.file?(File.join(state, 'releases/gitops-active'))
      GitopsControl.handle('complete', @release)
      refute File.file?(File.join(state, 'releases/gitops-active'))
      assert_equal %w[resume failed complete], File.readlines(File.join(state, 'releases/phases'), chomp: true)
    end
  ensure
    ENV['HOME'] = previous_home
  end

  def test_host_control_stops_when_git_desired_release_differs
    previous_home = ENV['HOME']
    ENV['HOME'] = @directory
    git = lambda do |*arguments|
      case arguments.first
      when 'remote' then 'https://github.com/ljkhyeong/happyGallery.git'
      when 'show' then JSON.generate(@release.merge('source_sha' => 'e' * 40))
      when 'fetch' then ''
      else flunk '다른 희망 상태의 배포 스크립트는 실행하지 않는다.'
      end
    end
    GitopsControl.stub(:git, git) do
      assert_raises(RuntimeError) { GitopsControl.handle('prepare', @release) }
    end
  ensure
    ENV['HOME'] = previous_home
  end

  def test_generated_manifest_matches_server_render_and_preserves_data_on_prune
    environment = { 'GITHUB_SHA' => @release['source_sha'], 'APP_REGISTRY_DIGEST' => @release['app_digest'],
                    'FRONTEND_REGISTRY_DIGEST' => @release['frontend_digest'], 'ACME_EMAIL' => @release['acme_email'] }
    release = GitopsRelease.generate(@directory, environment)
    path = File.join(@directory, 'manifests.yaml')
    documents = YAML.load_stream(File.read(path)).compact
    refute documents.any? { |document| document['kind'] == 'Secret' }
    documents.each do |document|
      annotations = document.fetch('metadata').fetch('annotations')
      if %w[Namespace PersistentVolumeClaim StorageClass ClusterIssuer StatefulSet].include?(document['kind'])
        assert_equal 'Prune=false,Delete=false', annotations.fetch('argocd.argoproj.io/sync-options')
      end
      annotations.delete('argocd.argoproj.io/sync-options')
      annotations.delete('argocd.argoproj.io/sync-wave')
      document.fetch('metadata').delete('annotations') if annotations.empty?
    end
    File.write(path, documents.reverse.map { |document| YAML.dump(document) }.join)
    assert_equal release.fetch('manifest_sha256'), GitopsRelease.manifest_digest(path)
    hooks = YAML.load_stream(File.read(File.join(@directory, 'hooks.yaml')))
    assert_equal %w[PreSync PostSync SyncFail], hooks.map { |hook| hook.dig('metadata', 'annotations', 'argocd.argoproj.io/hook') }
    hooks.each do |hook|
      specification = hook.dig('spec', 'template', 'spec')
      assert_equal false, specification.fetch('automountServiceAccountToken')
      container = specification.fetch('containers').first
      assert_equal true, container.dig('volumeMounts', 0, 'readOnly')
      assert_equal "ghcr.io/ljkhyeong/happygallery-frontend@#{release.fetch('frontend_digest')}", container.fetch('image')
      assert_equal release, JSON.parse(container.dig('env', 0, 'value')).reject { |key, _| key == 'phase' }
    end
  end

  def git(*arguments)
    PublishGitops.git(*arguments)
  end

  def test_publication_keeps_source_and_release_history_and_rejects_stale_main
    Dir.chdir(@directory) do
      remote = File.join(@directory, 'remote.git')
      git('init', '--bare', '-q', remote)
      git('init', '-q', '-b', 'main', 'source')
      Dir.chdir('source') do
        git('config', 'user.email', 'test@example.com')
        git('config', 'user.name', '검사')
        git('remote', 'add', 'origin', remote)
        File.write('source.txt', 'first')
        git('add', 'source.txt')
        git('commit', '-qm', '첫 소스')
        first_source = git('rev-parse', 'HEAD')
        git('push', '-q', 'origin', 'main')
        output = File.join(@directory, 'release')
        FileUtils.mkdir_p(output)
        %w[manifests.yaml hooks.yaml release.json].each { |name| File.write(File.join(output, name), name) }
        PublishGitops.publish(output, first_source)
        first_release = git('ls-remote', 'origin', 'refs/heads/gitops-production').split.first
        assert_equal %w[production/hooks.yaml production/manifests.yaml production/release.json],
                     git('ls-tree', '-r', '--name-only', first_release).split("\n")
        File.write('source.txt', 'second')
        git('commit', '-qam', '다음 소스')
        second_source = git('rev-parse', 'HEAD')
        git('push', '-q', 'origin', 'main')
        assert_raises(RuntimeError) { PublishGitops.publish(output, first_source) }
        PublishGitops.publish(output, second_source)
        second_release = git('ls-remote', 'origin', 'refs/heads/gitops-production').split.first
        git('merge-base', '--is-ancestor', first_release, second_release)
        git('merge-base', '--is-ancestor', second_source, second_release)
        refute_equal first_release, second_release
      end
    end
  end
end
