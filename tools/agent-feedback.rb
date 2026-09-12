#!/usr/bin/env ruby
# 파일 검사와 종료 검사를 Codex 훅·수동 실행에서 함께 사용한다.
require 'digest'
require 'fileutils'
require 'json'
require 'open3'
require 'tempfile'

class AgentFeedback
  class CheckFailed < StandardError; end

  attr_reader :root, :messages

  def initialize(root)
    @root = File.expand_path(root)
    @messages = []
  end

  def git(*args)
    output, error, status = Open3.capture3('git', '-C', root, *args)
    raise CheckFailed, error unless status.success?
    output
  end

  def changed_paths(base)
    [git('diff', '--name-only', '--no-renames', '-z', base, '--'),
     git('diff', '--cached', '--name-only', '--no-renames', '-z', base, '--'),
     git('ls-files', '--others', '--exclude-standard', '-z')].flat_map { |s| s.split("\0") }.uniq.sort
  end

  def snapshot(base)
    changed_paths(base).to_h do |path|
      file = File.join(root, path)
      value = if File.symlink?(file)
                File.readlink(file)
              elsif File.file?(file)
                Digest::SHA256.file(file).hexdigest
              else
                'deleted'
              end
      [path, value]
    end
  end

  def run(*command, directory: root, allowed: [0])
    FileUtils.mkdir_p(File.join(root, '.gradle/agent-feedback'))
    log = Tempfile.create(['check-', '.log'], File.join(root, '.gradle/agent-feedback'))
    pid = Process.spawn(*command, chdir: directory, out: log, err: log)
    _, status = Process.wait2(pid)
    log.close
    label = command.join(' ')
    unless allowed.include?(status.exitstatus)
      detail = File.readlines(log.path).last(35).join
      raise CheckFailed, "실패: #{label}\n로그: #{log.path}\n#{detail}"
    end
    messages << "통과: #{label} (로그: #{log.path})"
  rescue Errno::ENOENT => error
    raise CheckFailed, "실행 환경 확인 필요: #{error.message}"
  ensure
    log&.close unless log&.closed?
  end

  def frontend_lint?(path)
    path.match?(%r{\Afrontend/(src/(?!generated/).+\.tsx?|tests/.+\.(ts|mjs)|[^/]+\.config\.ts|server\.mjs)\z})
  end

  def structural?(paths)
    paths.any? do |path|
      path.match?(%r{\A[^/]+/src/main/.*\.java\z}) ||
        path.end_with?('LayerDependencyPolicyTest.java', '.gradle', '.gradle.kts') ||
        path.start_with?('gradle/') || %w[gradlew gradle.properties].include?(path)
    end
  end

  def local(paths, base: 'HEAD', compile: true)
    return if paths.empty?
    run('git', 'diff', '--check', base, '--', *paths)
    run('git', 'diff', '--cached', '--check', base, '--', *paths)
    untracked = git('ls-files', '--others', '--exclude-standard', '-z').split("\0")
    files = paths.select { |p| File.file?(File.join(root, p)) && !File.symlink?(File.join(root, p)) }
    (files & untracked).each do |path|
      run('git', 'diff', '--no-index', '--check', '--', '/dev/null', path, allowed: [0, 1])
    end
    files.grep(/\.rb\z/).each { |p| run('ruby', '-c', p) }
    files.grep(/\.sh\z/).each { |p| run('bash', '-n', p) }
    files.grep(/\.json\z/).each do |path|
      JSON.parse(File.read(File.join(root, path)))
    rescue JSON::ParserError => error
      raise CheckFailed, "#{path}: #{error.message}"
    end
    lint_files = files.select { |p| frontend_lint?(p) }.map { |p| p.delete_prefix('frontend/') }
    unless lint_files.empty?
      run('./node_modules/.bin/eslint', '--max-warnings', '0', '--', *lint_files,
          directory: File.join(root, 'frontend'))
    end
    if compile
      tasks = paths.map do |path|
        match = path.match(%r{\A([^/]+)/src/(main|test|testFixtures)/.*\.java\z})
        next unless match
        task = { 'main' => 'compileJava', 'test' => 'compileTestJava', 'testFixtures' => 'compileTestFixturesJava' }.fetch(match[2])
        ":#{match[1]}:#{task}"
      end.compact.uniq
      run('./gradlew', '--console=plain', *tasks) unless tasks.empty?
    end
  end

  def final(base)
    paths = changed_paths(base)
    return messages << '변경 없음' if paths.empty?
    review = File.join(root, '.gradle/agent-feedback/review.diff')
    FileUtils.mkdir_p(File.dirname(review))
    File.open(review, 'w') do |file|
      file.puts "기준: #{base}\n작업 트리 전체 변경"
      file.write(git('diff', '--no-ext-diff', '--no-textconv', base, '--'))
      file.puts "\n스테이징 변경"
      file.write(git('diff', '--cached', '--no-ext-diff', '--no-textconv', base, '--'))
      git('ls-files', '--others', '--exclude-standard', '-z').split("\0").each do |path|
        output, _error, status = Open3.capture3('git', 'diff', '--no-index', '--', '/dev/null', path, chdir: root)
        raise CheckFailed, "새 파일 diff 실패: #{path}" unless [0, 1].include?(status.exitstatus)
        file.write(output)
      end
    end
    messages << "전체 diff 검토: #{review} (#{paths.length}개 파일, 새 파일·스테이징 포함)"
    architecture = structural?(paths)
    local(paths, base: base, compile: !architecture)
    run('./gradlew', '--console=plain', ':application:architectureTest') if architecture
    if paths.any? { |p| p.start_with?('frontend/') && p.match?(/\.(tsx?|json|mjs)\z/) }
      run('npm', 'run', 'typecheck', directory: File.join(root, 'frontend'))
    end
    if paths.any? { |p| p.start_with?('.agents/') || %w[AGENTS.md CLAUDE.md tools/check-agent-skills.rb].include?(p) }
      run('ruby', 'tools/check-agent-skills.rb')
    end
    if paths.any? { |p| p.start_with?('tools/agent-feedback') || p == '.codex/hooks.json' }
      run('ruby', 'tools/agent-feedback-test.rb')
    end
    messages << '구조 검사 완료. 업무 정책·API 계약·동작 테스트와 전체 diff 의미 검토는 변경 범위에 맞게 별도로 확인한다.'
  end

  def hook(event)
    return {} if event['permission_mode'] == 'plan'
    folder = File.join(root, '.gradle/agent-feedback')
    FileUtils.mkdir_p(folder)
    state_path = File.join(folder, "#{Digest::SHA256.hexdigest(event.fetch('session_id'))}.json")
    File.open(File.join(folder, 'hook.lock'), 'w') do |lock|
      lock.flock(File::LOCK_EX)
      state = File.exist?(state_path) ? JSON.parse(File.read(state_path)) : {}
      result = handle_event(event, state)
      File.write(state_path, JSON.generate(state))
      result
    end
  end

  def handle_event(event, state)
    name = event.fetch('hook_event_name')
    if state.empty? || (name == 'UserPromptSubmit' && state['finished'])
      state.replace('base' => git('rev-parse', 'HEAD').strip, 'finished' => false)
      state['seen'] = name == 'UserPromptSubmit' ? snapshot(state['base']) : {}
    end
    return {} if name == 'UserPromptSubmit'
    current = snapshot(state.fetch('base'))
    if name == 'PostToolUse'
      previous = state.fetch('seen', {})
      paths = (current.keys | previous.keys).select { |p| current[p] != previous[p] }
      state['seen'] = current
      return {} if paths.empty?
      begin
        local(paths, base: state['base'])
        { 'hookSpecificOutput' => { 'hookEventName' => name, 'additionalContext' => messages.join("\n") } }
      rescue CheckFailed => error
        { 'hookSpecificOutput' => { 'hookEventName' => name, 'additionalContext' => "파일 검사 미통과. 원인을 수정한 뒤 해당 검사만 다시 실행하세요.\n#{error.message}" } }
      end
    elsif name == 'Stop'
      # 스테이징만 바뀐 경우도 종료 검사에서 놓치지 않는다.
      inputs = [current, ENV.values_at('JAVA_HOME', 'GRADLE_USER_HOME', 'PATH')]
      signature = Digest::SHA256.hexdigest(JSON.generate(inputs) + git('diff', '--cached', state['base'], '--'))
      if event['stop_hook_active'] && state['failed_signature'] == signature
        state['finished'] = true
        return { 'systemMessage' => "종료 검사 미통과 상태가 그대로입니다. 자동 재시도를 멈춥니다. 미확인 범위와 실패 이유를 최종 결과에 명시하세요.\n#{state['failure']}" }
      end
      begin
        final(state['base'])
        state['finished'] = true
        state.delete('failed_signature')
        {}
      rescue CheckFailed => error
        state['failed_signature'] = signature
        state['failure'] = error.message
        state['finished'] = false
        { 'decision' => 'block', 'reason' => "종료 검사 실패. 원인을 해결하고 필요한 검사만 다시 실행하세요.\n#{error.message}" }
      end
    else
      {}
    end
  end
end

if $PROGRAM_NAME == __FILE__
  mode = ARGV.shift
  feedback = AgentFeedback.new(File.expand_path('..', __dir__))
  begin
    case mode
    when 'local'
      abort '사용법: ruby tools/agent-feedback.rb local <저장소 기준 파일 경로...>' if ARGV.empty?
      feedback.local(ARGV)
      puts feedback.messages
    when 'final'
      feedback.final(ARGV.fetch(0, 'HEAD'))
      puts feedback.messages
    when 'hook'
      puts JSON.generate(feedback.hook(JSON.parse($stdin.read)))
    else
      abort '사용법: ruby tools/agent-feedback.rb local <파일...> | final [작업 시작 SHA] | hook'
    end
  rescue AgentFeedback::CheckFailed, JSON::ParserError, KeyError, SystemCallError => error
    if mode == 'hook'
      puts JSON.generate('systemMessage' => "검증 훅 실행 실패. 수동 local/final 검사가 필요합니다.\n#{error.message}")
    else
      warn error.message
      exit 1
    end
  end
end
