# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'fileutils'
require 'open3'
require 'digest'
require 'time'

class RcloneBackupTest < Minitest::Test
  SCRIPT = File.expand_path('../rclone-backup.sh', __dir__)
  TAG = 'a' * 40
  DIGEST = "sha256:#{'b' * 64}"

  def setup
    @dir = Dir.mktmpdir('happygallery-rclone-test')
    @local = File.join(@dir, 'local')
    @remote = File.join(@dir, 'remote', 'bucket', 'happygallery')
    @bin = File.join(@dir, 'bin')
    FileUtils.mkdir_p([@local, @remote, @bin])
    @log = File.join(@dir, 'calls')
    @config = File.join(@dir, 'rclone.conf')
    private_write(@config, "[teststore]\ntype = alias\nremote = #{File.join(@dir, 'remote')}\n")
    fake = File.join(@bin, 'rclone')
    File.write(fake, <<~'RUBY')
      #!/usr/bin/env ruby
      require 'fileutils'
      commands = %w[copy copyto check lsf delete]
      index = ARGV.index { |arg| commands.include?(arg) }
      abort 'unknown command' unless index
      command, *args = ARGV[index..-1]
      File.open(ENV.fetch('HG_RCLONE_TEST_LOG'), 'a') { |file| file.puts(command) }
      abort 'injected transport failure' if ENV['HG_RCLONE_TEST_FAIL'] == command
      if ENV['HG_TEST_REAL_RCLONE']
        exec(ENV.fetch('HG_TEST_REAL_RCLONE'), *ARGV)
      end
      map = lambda do |path|
        path.start_with?('teststore:') ? File.join(ENV.fetch('HG_RCLONE_TEST_ROOT'), path.delete_prefix('teststore:')) : path
      end
      list_index = args.index('--files-from-raw')
      names = list_index ? File.readlines(args.fetch(list_index + 1), chomp: true) : []
      copy = lambda do |source, target|
        abort 'missing file' unless File.file?(source)
        if args.include?('--immutable') && File.exist?(target) && File.binread(source) != File.binread(target)
          abort 'immutable conflict'
        end
        FileUtils.mkdir_p(File.dirname(target))
        FileUtils.cp(source, target)
      end
      case command
      when 'copyto'
        copy.call(map.call(args.fetch(0)), map.call(args.fetch(1)))
      when 'copy', 'check'
        names.each do |name|
          source, target = args.first(2).map { |root| File.join(map.call(root), name) }
          if command == 'copy'
            copy.call(source, target)
          else
            abort 'content mismatch' unless File.file?(source) && File.file?(target) && File.binread(source) == File.binread(target)
          end
        end
      when 'lsf'
        puts Dir.children(map.call(args.fetch(0))).select { |name| File.file?(File.join(map.call(args[0]), name)) }
      when 'delete'
        names.each { |name| FileUtils.rm_f(File.join(map.call(args.fetch(0)), name)) }
      end
    RUBY
    File.chmod(0o700, fake)
    # macOS에서도 보존 정책을 검사하도록 flock만 stub한다. 실제 잠금은 Ubuntu에서 제공한다.
    File.write(File.join(@bin, 'flock'), "#!/bin/sh\nexit 0\n")
    File.chmod(0o700, File.join(@bin, 'flock'))
    @env = {
      'PATH' => "#{@bin}:#{ENV.fetch('PATH')}", 'RCLONE_BIN' => fake,
      'RCLONE_CONFIG' => @config, 'RCLONE_BACKUP_REMOTE' => 'teststore:bucket/happygallery',
      'BACKUP_DIR' => @local, 'BACKUP_RETENTION_DAYS' => '30',
      'HG_RCLONE_TEST_LOG' => @log, 'HG_RCLONE_TEST_ROOT' => File.join(@dir, 'remote')
    }
    @name = create_bundle(Time.now.utc.strftime('%Y%m%dT%H%M%SZ'))
  end

  def teardown
    FileUtils.remove_entry(@dir)
  end

  def private_write(path, contents)
    FileUtils.mkdir_p(File.dirname(path))
    File.write(path, contents)
    File.chmod(0o600, path)
  end

  def with_checksum(path, contents)
    private_write(path, contents)
    private_write("#{path}.sha256", "#{Digest::SHA256.file(path).hexdigest}  #{File.basename(path)}\n")
  end

  def create_bundle(timestamp)
    prefix = "happygallery-#{timestamp}"
    with_checksum(File.join(@local, "#{prefix}.sql.gz.age"), 'encrypted database fixture')
    with_checksum(File.join(@local, "#{prefix}.media.tar.gz.age"), 'encrypted media fixture')
    release = File.join(@local, 'releases', TAG)
    with_checksum(File.join(release, 'metadata.env'), "IMAGE_TAG=#{TAG}\nAPP_IMAGE_DIGEST=#{DIGEST}\nFRONTEND_IMAGE_DIGEST=#{DIGEST}\n")
    with_checksum(File.join(release, 'manifests.yaml'), "kind: Deployment\n")
    with_checksum(File.join(release, 'runtime-images.env'), "MYSQL_IMAGE=mysql:8.4\n")
    with_checksum(File.join(release, 'images.tar'), 'runtime image archive fixture')
    name = "#{prefix}.recovery.env"
    with_checksum(File.join(@local, name), <<~ENV)
      BACKUP_CREATED_AT=#{timestamp}
      DATABASE_BACKUP=#{prefix}.sql.gz.age
      MEDIA_BACKUP=#{prefix}.media.tar.gz.age
      RELEASE_DIR=releases/#{TAG}
      IMAGE_TAG=#{TAG}
      APP_IMAGE_DIGEST=#{DIGEST}
      FRONTEND_IMAGE_DIGEST=#{DIGEST}
    ENV
    name
  end

  def run_script(*args, env: {})
    Open3.capture2e(@env.merge(env), 'bash', SCRIPT, *args)
  end

  def upload(name = @name)
    output, status = run_script('upload', File.join(@local, name))
    assert status.success?, output
  end

  def prepare_cd
    config = File.join(@dir, 'cd-backup.env')
    private_write(config, @env.slice('RCLONE_BIN', 'RCLONE_CONFIG', 'RCLONE_BACKUP_REMOTE').map { |k, v| "#{k}=#{v}\n" }.join)
    Open3.capture3(@env.merge('TZ' => 'Asia/Seoul'), 'bash', File.expand_path('../prepare-cd-backup.sh', __dir__), config, File.join(@dir, 'cd-cache'))
  end

  def test_cd_downloads_recent_bundle_and_rechecks_cached_contents
    upload
    path, error, status = prepare_cd
    assert status.success?, error
    assert File.file?(path.strip), path
    _path, error, status = prepare_cd
    assert status.success?, error
    File.write(File.join(File.dirname(path.strip), @name.sub('.recovery.env', '.media.tar.gz.age')), 'corrupt cache')
    _path, _error, status = prepare_cd
    refute status.success?
  end

  def test_cd_rejects_old_or_future_remote_backup_even_with_fresh_local_mtime
    [-49 * 3600, 3600].each do |offset|
      FileUtils.rm_rf(Dir.glob(File.join(@remote, '*')))
      name = create_bundle((Time.now.utc + offset).strftime('%Y%m%dT%H%M%SZ'))
      upload(name)
      _path, error, status = prepare_cd
      refute status.success?
      assert_includes error, '48시간보다 오래됐거나 미래'
    end
  end

  def test_cd_rejects_corrupt_remote_bundle
    upload
    File.write(File.join(@remote, @name.sub('.recovery.env', '.media.tar.gz.age')), 'corrupt remote')
    _path, _error, status = prepare_cd
    refute status.success?
    assert_empty Dir.glob(File.join(@dir, 'cd-cache', '*', '*.recovery.env'))
  end

  def test_upload_then_download_verifies_all_bundle_files
    upload
    assert_equal %w[copy check copyto check], File.readlines(@log, chomp: true)
    destination = File.join(@dir, 'restored')
    output, status = run_script('download', @name, destination)
    assert status.success?, output
    files = Dir.glob(File.join(@local, '**', '*')).select { |path| File.file?(path) }
    assert_equal 14, files.size
    files.each do |path|
      restored = File.join(destination, path.delete_prefix("#{@local}/"))
      assert_equal File.binread(path), File.binread(restored)
      assert_equal 0, File.stat(restored).mode & 0o077
    end
  end

  def test_failed_remote_content_check_does_not_publish_completion_marker
    output, status = run_script('upload', File.join(@local, @name), env: { 'HG_RCLONE_TEST_FAIL' => 'check' })
    refute status.success?, output
    refute File.exist?(File.join(@remote, @name))
    assert File.exist?(File.join(@remote, @name.sub('.recovery.env', '.sql.gz.age')))
    upload
    assert File.exist?(File.join(@remote, @name))
  end

  def test_corrupt_local_backup_is_rejected_before_upload
    private_write(File.join(@local, @name.sub('.recovery.env', '.sql.gz.age')), 'corrupt')
    _, status = run_script('upload', File.join(@local, @name))
    refute status.success?
    refute File.exist?(@log)
  end

  def test_existing_remote_object_is_not_overwritten_on_conflict
    path = File.join(@remote, @name.sub('.recovery.env', '.sql.gz.age'))
    private_write(path, 'different existing backup')
    _, status = run_script('upload', File.join(@local, @name))
    refute status.success?
    assert_equal 'different existing backup', File.read(path)
    refute File.exist?(File.join(@remote, @name))
  end

  def test_corrupt_download_never_appears_as_completed_directory
    upload
    private_write(File.join(@remote, @name.sub('.recovery.env', '.media.tar.gz.age')), 'corrupt')
    destination = File.join(@dir, 'restored')
    _, status = run_script('download', @name, destination)
    refute status.success?
    refute File.exist?(destination)
    assert_empty Dir.glob("#{destination}.partial.*")
  end

  def test_rejects_path_traversal_and_existing_download_target
    upload
    path = File.join(@remote, @name)
    with_checksum(path, File.read(path).sub(/DATABASE_BACKUP=.*/, 'DATABASE_BACKUP=../../private.env'))
    _, status = run_script('download', @name, File.join(@dir, 'restored'))
    refute status.success?
    _, status = run_script('download', @name, @local)
    refute status.success?
    assert File.exist?(File.join(@local, @name))
  end

  def test_retention_preserves_recent_backups_shared_images_and_unrelated_objects
    upload
    old = create_bundle((Time.now.utc - 40 * 86_400).strftime('%Y%m%dT%H%M%SZ'))
    upload(old)
    private_write(File.join(@remote, 'operator-notes.txt'), 'keep')
    output, status = run_script('prune')
    assert status.success?, output
    refute File.exist?(File.join(@remote, old))
    refute File.exist?(File.join(@local, old))
    assert File.exist?(File.join(@remote, @name))
    assert File.exist?(File.join(@remote, 'releases', TAG, 'images.tar'))
    assert_equal 'keep', File.read(File.join(@remote, 'operator-notes.txt'))
  end

  def test_retention_requires_recent_completion_and_keeps_local_files_if_remote_delete_fails
    old = create_bundle((Time.now.utc - 40 * 86_400).strftime('%Y%m%dT%H%M%SZ'))
    upload(old)
    _, status = run_script('prune')
    refute status.success?
    assert File.exist?(File.join(@remote, old))
    upload
    _, status = run_script('prune', env: { 'HG_RCLONE_TEST_FAIL' => 'delete' })
    refute status.success?
    assert File.exist?(File.join(@local, old))
  end

  def test_config_requires_private_permissions_and_dedicated_remote_prefix
    _, status = run_script('check', env: { 'RCLONE_BACKUP_REMOTE' => 'teststore:bucket' })
    refute status.success?
    File.chmod(0o644, @config)
    _, status = run_script('check')
    refute status.success?
    refute File.exist?(@log)
  end
end
