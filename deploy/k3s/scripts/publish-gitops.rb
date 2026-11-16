# frozen_string_literal: true

require 'tmpdir'
require_relative 'gitops-release'

# 이미지 검증을 통과한 main 소스만 배포 브랜치로 승격한다. 일반 push의 fast-forward 검사로 경합을 감지한다.
module PublishGitops
  def self.git(*arguments, input: nil, environment: {})
    output, error, status = Open3.capture3(environment, 'git', *arguments, stdin_data: input)
    raise "git #{arguments.first} 실패: #{error}" unless status.success?
    output.strip
  end

  def self.publish(directory, source_sha)
    branch = "refs/heads/#{GitopsRelease::BRANCH}"
    latest = git('ls-remote', 'origin', 'refs/heads/main').split.first
    raise '최신 main 소스만 승격합니다.' unless latest == source_sha
    previous = git('ls-remote', 'origin', branch).split.first
    git('fetch', '--no-tags', 'origin', branch) if previous
    Dir.mktmpdir('happygallery-gitops-index-') do |temporary|
      environment = { 'GIT_INDEX_FILE' => File.join(temporary, 'index') }
      git('read-tree', '--empty', environment: environment)
      %w[manifests.yaml hooks.yaml release.json].each do |name|
        blob = git('hash-object', '-w', '--stdin', input: File.binread(File.join(directory, name)))
        git('update-index', '--add', '--cacheinfo', "100644,#{blob},production/#{name}", environment: environment)
      end
      tree = git('write-tree', environment: environment)
      parents = ['-p', source_sha]
      parents = ['-p', previous, *parents] if previous
      commit = git('commit-tree', tree, *parents, input: "Chore: 검증한 운영 이미지를 GitOps에 반영\n",
                   environment: { 'GIT_AUTHOR_NAME' => 'github-actions[bot]', 'GIT_AUTHOR_EMAIL' => '41898282+github-actions[bot]@users.noreply.github.com',
                                  'GIT_COMMITTER_NAME' => 'github-actions[bot]', 'GIT_COMMITTER_EMAIL' => '41898282+github-actions[bot]@users.noreply.github.com' })
      git('push', 'origin', "#{commit}:#{branch}")
      puts "GitOps 승격 완료: #{source_sha} → #{commit}"
    end
  end
end

if $PROGRAM_NAME == __FILE__
  begin
    raise 'main에서만 승격합니다.' unless ENV['GITHUB_REF'] == 'refs/heads/main'
    PublishGitops.publish(ARGV.fetch(0), ENV.fetch('GITHUB_SHA'))
  rescue StandardError => error
    abort "오류: #{error.message}"
  end
end
