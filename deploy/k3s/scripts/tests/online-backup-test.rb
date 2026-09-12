# frozen_string_literal: true

require 'minitest/autorun'
require 'fileutils'
require 'open3'
require 'tmpdir'
require 'timeout'

class OnlineBackupTest < Minitest::Test
  SCRIPT = File.expand_path('../backup-data-online.sh', __dir__)
  OLD_IMAGE = '11111111-1111-1111-1111-111111111111.png'
  NEW_IMAGE = '22222222-2222-2222-2222-222222222222.jpg'

  def setup
    @directory = Dir.mktmpdir('happygallery-online-backup-')
    @media = File.join(@directory, 'media')
    @scratch = File.join(@directory, 'scratch')
    @commands = File.join(@directory, 'commands.log')
    @db = File.join(@directory, 'db.age')
    @archive = File.join(@directory, 'media.age')
    @guard = File.join(@media, '.backup-in-progress')
    FileUtils.mkdir_p([@media, @scratch, File.join(@media, '.orphaned')])
    File.write(File.join(@media, OLD_IMAGE), 'old image')
    File.write(File.join(@media, 'upload-partial.tmp'), 'incomplete upload')
    @env = { 'PATH' => "#{@directory}:#{ENV.fetch('PATH')}",
             'KUBECTL_BIN' => File.join(@directory, 'kubectl'),
             'BACKUP_AGE_RECIPIENT' => 'test-recipient',
             'MYSQL_ROOT_PASSWORD' => 'test-only', 'MYSQL_DATABASE' => 'happygallery',
             'ONLINE_TEST_MEDIA' => @media, 'ONLINE_TEST_SCRATCH' => @scratch,
             'ONLINE_TEST_COMMANDS' => @commands, 'ONLINE_TEST_FAILURE' => '' }
    executable('kubectl', <<~'RUBY')
      File.open(ENV.fetch('ONLINE_TEST_COMMANDS'), 'a') { |file| file.puts ARGV.join(' ') }
      failure = ENV.fetch('ONLINE_TEST_FAILURE')
      if (separator = ARGV.index('--')) && ARGV.include?('exec')
        command = ARGV.drop(separator + 1)
        if command[0] == 'test'
          exit(failure == 'old-app' ? 1 : 0)
        end
        if command[0..1] == ['sh', '-ec']
          script = command[2]
          exit 44 if failure == 'release' && script.include?('rmdir /media/.backup-in-progress')
          script = script.gsub(%r{/(media|tmp)(?=/|\s|$)}) do |prefix|
            ENV.fetch(prefix == '/media' ? 'ONLINE_TEST_MEDIA' : 'ONLINE_TEST_SCRATCH')
          end
          command[2] = script
        end
        exec(*command)
      elsif ARGV.include?('apply')
        STDIN.read
      elsif (ARGV & %w[get wait delete]).empty?
        abort "unexpected kubectl mutation: #{ARGV.inspect}"
      end
    RUBY
    executable('mysql', <<~'RUBY')
      sql = ARGV.find { |arg| arg.start_with?('--execute=') }.to_s
      if sql.include?('ENGINE')
        puts ENV.fetch('ONLINE_TEST_FAILURE') == 'engine' ? 1 : 0
      elsif sql.include?('FOR UPDATE')
        abort 'guard missing before barrier' unless File.directory?(File.join(ENV.fetch('ONLINE_TEST_MEDIA'), '.backup-in-progress'))
        exit 23 if ENV.fetch('ONLINE_TEST_FAILURE') == 'barrier'
        File.write(File.join(ENV.fetch('ONLINE_TEST_SCRATCH'), 'barrier'), 'passed')
        puts 1
      else
        abort "unexpected SQL: #{sql}"
      end
    RUBY
    executable('mysqldump', <<~'RUBY')
      abort 'snapshot flag missing' unless %w[--single-transaction --quick --skip-lock-tables].all? { |flag| ARGV.include?(flag) }
      media = ENV.fetch('ONLINE_TEST_MEDIA')
      abort 'guard missing during snapshot' unless File.directory?(File.join(media, '.backup-in-progress'))
      abort 'barrier missing' unless File.file?(File.join(ENV.fetch('ONLINE_TEST_SCRATCH'), 'barrier'))
      puts 'snapshot SQL'
      exit 23 if ENV.fetch('ONLINE_TEST_FAILURE') == 'dump'
      if ENV.fetch('ONLINE_TEST_FAILURE') == 'signal'
        File.write(File.join(ENV.fetch('ONLINE_TEST_SCRATCH'), 'dump-running'), 'ready')
        sleep 60
      end
      unless ENV.fetch('ONLINE_TEST_FAILURE') == 'empty'
        File.write(File.join(media, '22222222-2222-2222-2222-222222222222.jpg'), 'concurrent upload')
      end
    RUBY
    executable('age', <<~'RUBY')
      # 암호화 경계는 모사하고 실제 gzip/tar 스트림을 검사한다.
      File.open(ARGV.fetch(ARGV.index('-o') + 1), 'wb') { |file| IO.copy_stream(STDIN, file) }
    RUBY
    executable('tar', <<~'RUBY')
      exit 25 if ARGV.include?('-cf') && ENV.fetch('ONLINE_TEST_FAILURE') == 'tar'
      exec('/usr/bin/tar', *ARGV)
    RUBY
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def executable(name, body)
    path = File.join(@directory, name)
    File.write(path, "#!/usr/bin/env ruby\n#{body}")
    File.chmod(0o755, path)
  end

  def backup(replicas = '1')
    Open3.capture3(@env, 'bash', SCRIPT, @db, @archive, 'example.invalid/app@sha256:test', replicas)
  end

  def test_online_snapshot_preserves_old_and_new_images_without_upload_temporary_files
    output, error, status = backup
    assert status.success?, error
    assert_includes output, 'app replica를 변경하지 않았습니다'
    listing, tar_error, tar_status = Open3.capture3('tar', '-tzf', @archive)
    assert tar_status.success?, tar_error
    assert_equal ["./#{OLD_IMAGE}", "./#{NEW_IMAGE}"].sort, listing.lines.map(&:strip).sort
    refute File.exist?(@guard)
    refute_match(/scale|rollout|CHECK TABLE/, File.read(@commands))
  end

  def test_snapshot_and_archive_failures_release_owned_guard_and_remove_partial_files
    %w[barrier dump tar].each do |failure|
      @env['ONLINE_TEST_FAILURE'] = failure
      _output, _error, status = backup
      refute status.success?, failure
      refute File.exist?(@guard), failure
      refute File.exist?(@db), failure
      refute File.exist?(@archive), failure
    end
  end

  def test_empty_media_produces_a_readable_empty_archive
    @env['ONLINE_TEST_FAILURE'] = 'empty'
    File.delete(File.join(@media, OLD_IMAGE))
    _output, error, status = backup
    assert status.success?, error
    listing, tar_error, tar_status = Open3.capture3('tar', '-tzf', @archive)
    assert tar_status.success?, tar_error
    assert_empty listing
    refute File.exist?(@guard)
  end

  def test_unknown_guard_is_not_released_by_another_backup
    FileUtils.mkdir_p(@guard)
    File.write(File.join(@guard, 'owner'), 'another-backup')
    _output, _error, status = backup
    refute status.success?
    assert_equal 'another-backup', File.read(File.join(@guard, 'owner'))
    refute File.exist?(@db)
  end

  def test_guard_release_failure_cannot_report_success
    @env['ONLINE_TEST_FAILURE'] = 'release'
    _output, _error, status = backup
    refute status.success?
    assert File.directory?(@guard)
    refute File.exist?(@db)
    refute File.exist?(@archive)
  end

  def test_termination_releases_guard_and_does_not_leave_a_completed_backup
    @env['ONLINE_TEST_FAILURE'] = 'signal'
    Open3.popen3(@env, 'bash', SCRIPT, @db, @archive,
                 'example.invalid/app@sha256:test', '1', pgroup: true) do |input, output, error, wait|
      input.close
      begin
        Timeout.timeout(10) do
          sleep 0.02 until File.exist?(File.join(@scratch, 'dump-running'))
        end
        Process.kill('TERM', -wait.pid)
        status = Timeout.timeout(10) { wait.value }
        refute status.success?, output.read + error.read
        refute File.exist?(@guard)
        refute File.exist?(@db)
        refute File.exist?(@archive)
      ensure
        Process.kill('KILL', -wait.pid) if wait.alive?
      end
    end
  end

  def test_old_app_and_non_innodb_tables_are_rejected_before_creating_a_guard
    %w[old-app engine].each do |failure|
      @env['ONLINE_TEST_FAILURE'] = failure
      _output, _error, status = backup
      refute status.success?, failure
      refute File.exist?(@guard), failure
      refute File.exist?(@db), failure
    end
  end

  def test_key_rotation_backup_keeps_already_stopped_app_stopped
    @env['ONLINE_TEST_FAILURE'] = 'old-app'
    _output, error, status = backup('0')
    assert status.success?, error
    refute_match(/scale|rollout|test -f \/app/, File.read(@commands))
  end
end
