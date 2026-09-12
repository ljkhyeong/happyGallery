# frozen_string_literal: true

require 'minitest/autorun'
require 'fileutils'
require 'json'
require 'open3'
require 'tmpdir'

class CdTest < Minitest::Test
  SCRIPTS = File.expand_path('..', __dir__)
  SHA = 'a' * 40
  DIGEST = "sha256:#{'b' * 64}"
  LOCAL_DIGEST = "sha256:#{'c' * 64}"

  def setup
    @dir = Dir.mktmpdir('happygallery-cd-test')
    @scripts = File.join(@dir, 'deploy/k3s/scripts')
    @bin = File.join(@dir, 'bin')
    FileUtils.mkdir_p([@scripts, @bin])
    %w[common.sh ci-deploy-ssh.sh ci-images.sh accept-cd-ssh.sh deploy-registry-images.sh update-release-images.rb].each do |name|
      FileUtils.cp(File.join(SCRIPTS, name), @scripts)
    end
    @log = File.join(@dir, 'calls')
    @env = { 'PATH' => "#{@bin}:#{ENV.fetch('PATH')}", 'HOME' => @dir, 'HG_CD_LOG' => @log,
             'HG_CD_SHA' => SHA, 'HG_CD_DIGEST' => DIGEST, 'GITHUB_SHA' => SHA,
             'GITHUB_REPOSITORY' => 'ljkhyeong/happyGallery', 'GITHUB_REF' => 'refs/heads/main',
             'CD_HOST' => 'example.test', 'CD_PORT' => '22222', 'CD_USER' => 'ronaldo',
             'APP_REGISTRY_DIGEST' => DIGEST, 'FRONTEND_REGISTRY_DIGEST' => DIGEST,
             'CD_SSH_PRIVATE_KEY' => 'private fixture', 'CD_SSH_KNOWN_HOSTS' => 'pinned host fixture' }
    executable(@bin, 'flock', 'exit 0')
    executable(@bin, 'git', <<~'RUBY')
      case ARGV.join(' ')
      when /remote get-url/ then puts 'https://github.com/ljkhyeong/happyGallery.git'
      when /rev-parse/ then puts ENV.fetch('HG_CD_SHA')
      when /status/ then puts ' M unexpected' if ENV['HG_CD_DIRTY']
      end
    RUBY
    executable(@bin, 'ssh', <<~'RUBY')
      key = ARGV.fetch(ARGV.index('-i') + 1)
      abort 'key permissions' unless File.stat(key).mode & 0o777 == 0o600
      abort 'key leaked in child env' if ENV['CD_SSH_PRIVATE_KEY']
      record(ARGV + [File.dirname(key)])
      exit Integer(ENV.fetch('HG_CD_SSH_EXIT', '0'))
    RUBY
    executable(@bin, 'docker', <<~'RUBY')
      record(ARGV)
      abort 'injected pull failure' if ARGV.first == 'pull' && ENV['HG_CD_PULL_FAIL']
      if ARGV.first == 'login'
        abort 'missing token stdin' if STDIN.read.empty?
      elsif ARGV.first(2) == %w[image inspect]
        if ARGV.include?('--format')
          puts JSON.generate([ARGV[2].split(':').first + '@' + ENV.fetch('HG_CD_DIGEST')])
        else
          puts JSON.generate([{ 'Architecture' => ENV.fetch('HG_CD_ARCH', 'amd64'), 'Os' => 'linux',
                                'RepoDigests' => [ENV.fetch('HG_CD_REF', ARGV[2])], 'Config' => { 'Labels' => {
                                  'org.opencontainers.image.revision' => ENV.fetch('HG_CD_LABEL', ENV.fetch('HG_CD_SHA')),
                                  'org.opencontainers.image.source' => 'https://github.com/ljkhyeong/happyGallery' } } }])
        end
      end
    RUBY
  end

  def teardown
    FileUtils.remove_entry(@dir)
  end

  def executable(root, name, body)
    File.write(File.join(root, name), <<~RUBY)
      #!/usr/bin/env ruby
      require 'json'
      def record(args)
        File.open(ENV.fetch('HG_CD_LOG'), 'a') { |f| f.puts JSON.generate([File.basename($0), *args]) }
      end
      #{body}
    RUBY
    File.chmod(0o700, File.join(root, name))
  end

  def run_script(name, *args)
    Open3.capture3(@env, 'bash', File.join(@scripts, name), *args)
  end

  def calls
    File.exist?(@log) ? File.readlines(@log).map { |line| JSON.parse(line) } : []
  end

  def test_ssh_pins_host_and_cleans_temporary_private_key_on_failure
    @env['HG_CD_SSH_EXIT'] = '255'
    _out, _error, status = run_script('ci-deploy-ssh.sh')
    assert_equal 255, status.exitstatus
    args = calls.fetch(0)
    assert_includes args, 'StrictHostKeyChecking=yes'
    assert_includes args, "deploy #{SHA} #{DIGEST} #{DIGEST}"
    refute File.exist?(args.last)
  end

  def test_invalid_revision_never_opens_ssh
    @env['GITHUB_SHA'] = "#{SHA}; touch /tmp/unwanted"
    _out, _error, status = run_script('ci-deploy-ssh.sh')
    refute status.success?
    assert_empty calls
  end

  def test_gateway_rejects_arbitrary_commands_and_stale_main
    ['bash', "deploy #{SHA} #{DIGEST} #{DIGEST}; id", "deploy #{'d' * 40} #{DIGEST} #{DIGEST}"].each do |command|
      @env['SSH_ORIGINAL_COMMAND'] = command
      _out, _error, status = run_script('accept-cd-ssh.sh')
      refute status.success?
    end
    assert_empty calls
  end

  def test_gateway_accepts_exact_main_without_changing_manual_checkout
    checkout = File.join(@dir, '.local/state/happygallery/cd/source')
    target = File.join(checkout, 'deploy/k3s/scripts')
    FileUtils.mkdir_p(target)
    File.write(File.join(target, 'deploy-registry-images.sh'), 'printf "%s\n" "$@"')
    @env['SSH_ORIGINAL_COMMAND'] = "deploy #{SHA} #{DIGEST} #{DIGEST}"
    out, error, status = run_script('accept-cd-ssh.sh')
    assert status.success?, error
    assert_equal ['/etc/happygallery/release.env', SHA, DIGEST, DIGEST], out.lines.map(&:strip)
  end

  def test_publish_emits_registry_digest_outputs_after_both_pushes
    @env.merge!('GHCR_TOKEN' => 'token fixture', 'GITHUB_ACTOR' => 'ljkhyeong', 'GITHUB_OUTPUT' => File.join(@dir, 'output'))
    _out, error, status = run_script('ci-images.sh', 'publish')
    assert status.success?, error
    assert_equal "app_digest=#{DIGEST}\nfrontend_digest=#{DIGEST}\n", File.read(@env['GITHUB_OUTPUT'])
    assert_equal 2, calls.count { |call| call[1] == 'push' }
  end

  def test_non_main_cannot_publish
    @env['GITHUB_REF'] = 'refs/pull/1/merge'
    _out, _error, status = run_script('ci-images.sh', 'publish')
    refute status.success?
    assert_empty calls
  end

  def prepare_registry
    # 백업 자체의 다운로드·무결성 검사는 rclone-backup-test에서 실제 스크립트로 검증한다.
    File.open(File.join(@scripts, 'common.sh'), 'a') { |f| f.puts "\nverify_recovery_bundle_files() { test -f \"$1\"; }" }
    bundle = File.join(@dir, 'bundle.env')
    File.write(bundle, '')
    File.write(File.join(@scripts, 'prepare-cd-backup.sh'), "printf '%s\\n' '#{bundle}'\n")
    executable(@scripts, 'verify-app-image.sh', 'record(ARGV)')
    executable(@scripts, 'deploy.sh', 'record(ARGV); abort "missing verified backup" unless File.file?(ENV.fetch("VERIFIED_RECOVERY_BUNDLE_OVERRIDE"))')
    @release = File.join(@dir, 'release.env')
    File.write(@release, "PUBLIC_HOST=happy-gallery.com\n")
    File.chmod(0o600, @release)
    executable(@bin, 'kube', "puts 'amd64'")
    @env['KUBECTL_BIN'] = File.join(@bin, 'kube')
    @env['K3S_BIN'] = File.join(@bin, 'k3s')
    executable(@bin, 'k3s', <<~RUBY)
      record(ARGV)
      if ARGV[1..2] == %w[images list]
        refs = %w[app frontend].flat_map do |name|
          ref = "localhost/happygallery-\#{name}:#{SHA}"
          [ref, "\#{ref}@#{LOCAL_DIGEST}", "localhost/happygallery-\#{name}@#{LOCAL_DIGEST}"]
        end
        if ARGV.include?('-q')
          puts refs
        else
          puts 'REF TYPE DIGEST SIZE'
          refs.each { |ref| puts "\#{ref} manifest #{LOCAL_DIGEST} 1MiB" }
        end
      end
    RUBY
  end

  def test_registry_import_uses_local_digest_and_existing_deploy_without_rebuilding
    prepare_registry
    _out, error, status = run_script('deploy-registry-images.sh', @release, SHA, DIGEST, DIGEST)
    assert status.success?, error
    assert_includes calls, ['deploy.sh', '--imported', @release, SHA]
    assert calls.any? { |call| call.first(3) == %w[docker pull --platform] }
    refute calls.any? { |call| call.first(2) == %w[docker build] }
    assert_equal "PUBLIC_HOST=happy-gallery.com\n", File.read(@release)
  end

  def test_wrong_commit_architecture_or_failed_pull_never_imports_or_deploys
    prepare_registry
    [{ 'HG_CD_LABEL' => 'd' * 40 }, { 'HG_CD_ARCH' => 'arm64' }, { 'HG_CD_REF' => 'unexpected@sha256:wrong' }, { 'HG_CD_PULL_FAIL' => '1' }].each do |bad|
      @env.merge!(bad)
      FileUtils.rm_f(@log)
      _out, _error, status = run_script('deploy-registry-images.sh', @release, SHA, DIGEST, DIGEST)
      refute status.success?
      refute calls.any? { |call| call.first == 'deploy.sh' || call.first(4) == %w[k3s ctr images import] }
      bad.each_key { |key| @env.delete(key) }
    end
  end
end
