#!/usr/bin/env ruby
require 'minitest/autorun'
require 'tmpdir'
require_relative 'agent-feedback'

class AgentFeedbackTest < Minitest::Test
  class RecordingFeedback < AgentFeedback
    attr_reader :commands
    attr_accessor :fail_architecture

    def initialize(root)
      super
      @commands = []
    end

    def run(*command, **options)
      commands << [command, options]
      if fail_architecture && command.include?(':application:architectureTest')
        raise CheckFailed, '의존 방향 위반'
      end
    end
  end

  def setup
    @root = Dir.mktmpdir('agent-feedback-test-')
    @feedback = RecordingFeedback.new(@root)
    @feedback.git('init', '-q')
    write('.gitignore', ".gradle/\n")
    write('README.md', "기존 문서\n")
    write('domain/src/main/java/Model.java', "class Model {}\n")
    commit
    @base = @feedback.git('rev-parse', 'HEAD').strip
  end

  def teardown
    FileUtils.remove_entry(@root)
  end

  def write(path, content)
    FileUtils.mkdir_p(File.dirname(File.join(@root, path)))
    File.write(File.join(@root, path), content)
  end

  def commit
    @feedback.git('add', '.')
    @feedback.git('-c', 'user.name=검증', '-c', 'user.email=test@example.invalid', 'commit', '-qm', '검증 준비')
  end

  def event(name, **extra)
    { 'session_id' => 'test-session', 'hook_event_name' => name, 'cwd' => @root,
      'permission_mode' => 'default' }.merge(extra.transform_keys(&:to_s))
  end

  def architecture_runs
    @feedback.commands.count { |command, _| command.include?(':application:architectureTest') }
  end

  def test_final_includes_committed_staged_unstaged_new_and_deleted_files
    write('committed.md', "중간 커밋\n")
    commit
    write('staged.md', "스테이징\n")
    @feedback.git('add', 'staged.md')
    write('README.md', "수정 문서\n")
    write('새 파일.md', "새 파일\n")
    File.delete(File.join(@root, 'domain/src/main/java/Model.java'))

    assert_equal ['README.md', 'committed.md', 'domain/src/main/java/Model.java', 'staged.md', '새 파일.md'],
                 @feedback.changed_paths(@base)
    @feedback.final(@base)
    review = File.read(File.join(@root, '.gradle/agent-feedback/review.diff'))
    %w[중간 스테이징 수정 새].each { |word| assert_includes review, word }
    assert_includes review, 'deleted file mode'
    assert_equal 1, architecture_runs
  end

  def test_local_batches_compilation_by_module_and_source_set
    paths = ['domain/src/main/java/A.java', 'domain/src/main/java/B.java',
             'application/src/test/java/A.java', 'application/src/testFixtures/java/F.java']
    @feedback.local(paths)
    commands = @feedback.commands.map(&:first).select { |c| c.first == './gradlew' }
    assert_equal [['./gradlew', '--console=plain', ':domain:compileJava',
                   ':application:compileTestJava', ':application:compileTestFixturesJava']], commands
    assert_equal 0, architecture_runs
  end

  def test_frontend_lints_changed_files_and_skips_generated_sources
    paths = ['frontend/src/page.tsx', 'frontend/src/generated/api/client.ts', 'frontend/tests/test.ts']
    paths.each { |p| write(p, 'export {}') }
    @feedback.local(paths)
    lint, options = @feedback.commands.find { |c, _| c.first.include?('eslint') }
    assert_equal ['./node_modules/.bin/eslint', '--max-warnings', '0', '--', 'src/page.tsx', 'tests/test.ts'], lint
    assert_equal File.join(@root, 'frontend'), options[:directory]
    refute @feedback.commands.any? { |c, _| c.include?('typecheck') }
  end

  def test_document_only_final_skips_build_and_typecheck
    write('README.md', "새 문서\n")
    @feedback.final(@base)
    refute @feedback.commands.any? { |c, _| %w[./gradlew npm].include?(c.first) }
  end

  def test_structural_final_does_not_compile_again_in_local_phase
    write('domain/src/main/java/Model.java', "class Model { int id; }\n")
    @feedback.final(@base)
    commands = @feedback.commands.map(&:first).select { |c| c.first == './gradlew' }
    assert_equal [['./gradlew', '--console=plain', ':application:architectureTest']], commands
  end

  def test_post_tool_ignores_existing_edits_reads_and_commits_but_checks_new_changes
    write('README.md', "작업 전 변경\n")
    @feedback.hook(event('UserPromptSubmit'))
    assert_empty @feedback.hook(event('PostToolUse'))
    assert_empty @feedback.commands
    write('domain/src/main/java/Model.java', "class Model { int id; }\n")
    result = @feedback.hook(event('PostToolUse'))
    assert_equal 'PostToolUse', result.dig('hookSpecificOutput', 'hookEventName')
    count = @feedback.commands.length
    @feedback.hook(event('PostToolUse'))
    commit
    @feedback.hook(event('PostToolUse'))
    assert_equal count, @feedback.commands.length
    @feedback.hook(event('Stop'))
    assert_equal 1, architecture_runs
  end

  def test_stop_retries_only_after_change_and_preserves_base_on_continuation
    @feedback.hook(event('UserPromptSubmit'))
    write('domain/src/main/java/Model.java', "class Model { int id; }\n")
    commit
    @feedback.fail_architecture = true
    result = @feedback.hook(event('Stop'))
    assert_equal 'block', result['decision']
    @feedback.hook(event('UserPromptSubmit'))
    result = @feedback.hook(event('Stop', stop_hook_active: true))
    assert_includes result.fetch('systemMessage'), '미통과'
    assert_equal 1, architecture_runs
    write('domain/src/main/java/Model.java', "class Model { long id; }\n")
    @feedback.fail_architecture = false
    assert_empty @feedback.hook(event('Stop', stop_hook_active: true))
    assert_equal 2, architecture_runs
  end

  def test_new_user_turn_resets_base_after_completion
    @feedback.hook(event('UserPromptSubmit'))
    write('domain/src/main/java/Model.java', "class Model { int id; }\n")
    @feedback.hook(event('Stop'))
    commit
    @feedback.hook(event('UserPromptSubmit'))
    @feedback.hook(event('Stop'))
    assert_equal 1, architecture_runs
  end

  def test_stop_retries_when_execution_environment_is_fixed
    previous_java = ENV['JAVA_HOME']
    @feedback.hook(event('UserPromptSubmit'))
    write('domain/src/main/java/Model.java', "class Model { int id; }\n")
    @feedback.fail_architecture = true
    assert_equal 'block', @feedback.hook(event('Stop'))['decision']
    ENV['JAVA_HOME'] = '/changed-jdk-for-test'
    @feedback.fail_architecture = false
    assert_empty @feedback.hook(event('Stop', stop_hook_active: true))
    assert_equal 2, architecture_runs
  ensure
    ENV['JAVA_HOME'] = previous_java
  end

  def test_plan_mode_does_not_run_checks
    assert_empty @feedback.hook(event('Stop', permission_mode: 'plan'))
    assert_empty @feedback.commands
  end

  def test_real_local_rejects_whitespace_in_untracked_file
    write('new.md', "문장  \n")
    error = assert_raises(AgentFeedback::CheckFailed) { AgentFeedback.new(@root).local(['new.md']) }
    assert_includes error.message, 'trailing whitespace'
  end

  def test_real_local_reports_invalid_json
    write('new.json', "{broken\n")
    error = assert_raises(AgentFeedback::CheckFailed) { AgentFeedback.new(@root).local(['new.json']) }
    assert_includes error.message, 'new.json'
  end
end
