# frozen_string_literal: true

require 'minitest/autorun'
require 'fileutils'
require 'open3'
require 'tmpdir'

class MysqlCheckTest < Minitest::Test
  COMMON = File.expand_path('../common.sh', __dir__)
  BACKUP = File.expand_path('../backup-mysql.sh', __dir__)

  def setup
    @directory = Dir.mktmpdir('happygallery-mysql-check-')
    @commands = File.join(@directory, 'commands.log')
    @env = {
      'PATH' => "#{@directory}:#{ENV.fetch('PATH')}",
      'KUBECTL_BIN' => File.join(@directory, 'kubectl'),
      'MYSQL_ROOT_PASSWORD' => 'test-only-password', 'MYSQL_DATABASE' => 'happygallery',
      'MYSQL_TEST_COMMANDS' => @commands,
      'MYSQL_TEST_STATEMENTS' => "CHECK TABLE `happygallery`.`first`;\nCHECK TABLE `happygallery`.`second`;\n",
      'MYSQL_TEST_RESULTS' => "happygallery.first\tcheck\tstatus\tOK\nhappygallery.second\tcheck\tstatus\tOK\n",
      'MYSQL_TEST_LIST_EXIT' => '0', 'MYSQL_TEST_CHECK_EXIT' => '0', 'MYSQL_TEST_DUMP_EXIT' => '0',
      'BACKUP_DIR' => @directory, 'BACKUP_AGE_RECIPIENT' => 'test-recipient', 'BACKUP_STORAGE' => 'mounted'
    }
    File.write(File.join(@directory, '.happygallery-off-device-backup-target'), '')
    executable('kubectl', <<~'RUBY')
      File.open(ENV.fetch('MYSQL_TEST_COMMANDS'), 'a') { |file| file.puts ARGV.join(' ') }
      if (separator = ARGV.index('--'))
        exec(*ARGV.drop(separator + 1))
      elsif ARGV.include?('get') && ARGV.any? { |arg| arg.include?('.spec.replicas') }
        puts '1'
      elsif ARGV.include?('get') || ARGV.include?('wait')
        exit 0
      else
        abort "예상하지 않은 kubectl 호출: #{ARGV.inspect}"
      end
    RUBY
    executable('mysql', <<~'RUBY')
      if ARGV.include?('--execute=SELECT 1')
        puts '1'
      else
        sql = STDIN.read
        if sql.include?('information_schema.TABLES')
          abort '대상 DB 제한 누락' unless sql.include?('TABLE_SCHEMA = DATABASE()')
          print ENV.fetch('MYSQL_TEST_STATEMENTS')
          exit ENV.fetch('MYSQL_TEST_LIST_EXIT').to_i
        else
          abort 'CHECK TABLE 입력 불일치' unless sql == ENV.fetch('MYSQL_TEST_STATEMENTS')
          print ENV.fetch('MYSQL_TEST_RESULTS')
          exit ENV.fetch('MYSQL_TEST_CHECK_EXIT').to_i
        end
      end
    RUBY
    executable('mysqldump', "exit ENV.fetch('MYSQL_TEST_DUMP_EXIT').to_i")
    executable('flock', 'exit 0')
    executable('age', 'exit 0')
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def executable(name, body)
    path = File.join(@directory, name)
    File.write(path, "#!/usr/bin/env ruby\n#{body}")
    File.chmod(0o755, path)
  end

  def check
    Open3.capture3(@env, 'bash', '-c', 'set -o pipefail; . "$1"; check_mysql_database', COMMON, COMMON)
  end

  def test_every_table_must_have_a_successful_status
    output, error, status = check
    assert status.success?, error
    assert_includes output, '검사 통과: 2 개'
  end

  def test_sql_error_rows_fail_even_when_mysql_exits_successfully
    @env['MYSQL_TEST_RESULTS'] = "happygallery.first\tcheck\terror\tCorrupt\n" + @env['MYSQL_TEST_RESULTS']
    _output, error, status = check
    refute status.success?
    assert_includes error, '테이블 검사 오류'
  end

  def test_failed_status_is_rejected
    @env['MYSQL_TEST_RESULTS'] = @env['MYSQL_TEST_RESULTS'].sub('OK', 'Operation failed')
    _output, error, status = check
    refute status.success?
    assert_includes error, '테이블 검사 실패'
  end

  def test_missing_and_duplicate_results_are_rejected
    first = @env['MYSQL_TEST_RESULTS'].lines.first
    [first, first * 2, ''].each do |results|
      @env['MYSQL_TEST_RESULTS'] = results
      _output, _error, status = check
      refute status.success?, results.inspect
    end
  end

  def test_empty_catalog_and_query_failures_are_rejected
    [{ 'MYSQL_TEST_STATEMENTS' => '' }, { 'MYSQL_TEST_LIST_EXIT' => '1' },
     { 'MYSQL_TEST_CHECK_EXIT' => '1' }].each do |failure|
      previous = @env.dup
      @env.update(failure)
      _output, _error, status = check
      refute status.success?, failure.inspect
      @env = previous
    end
  end

  def test_missing_dump_tool_stops_backup_before_app_scale_down
    @env['MYSQL_TEST_DUMP_EXIT'] = '127'
    _output, error, status = Open3.capture3(@env, 'bash', BACKUP)
    refute status.success?
    assert_includes error, '필수 도구 사전 검사'
    refute_includes File.read(@commands), 'scale'
  end
end
