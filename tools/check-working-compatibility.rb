#!/usr/bin/env ruby
# 사용자 인덱스를 건드리지 않고 미커밋·새 파일을 포함한 소스 호환성을 검사한다.
require 'tmpdir'
require_relative '../deploy/k3s/scripts/rolling-release'

module WorkingCompatibility
  def self.check(root, base)
    Dir.mktmpdir('happygallery-compatibility-') do |folder|
      environment = { 'GIT_INDEX_FILE' => File.join(folder, 'index') }
      git = lambda do |*args|
        output, status = Open3.capture2e(environment, 'git', '-C', root, *args)
        raise output unless status.success?
        output.strip
      end
      git.call('read-tree', 'HEAD')
      paths = RollingRelease::COMPATIBILITY_PATHS.select do |path|
        File.exist?(File.join(root, path)) || !git.call('ls-files', '--', path).empty?
      end
      git.call('add', '-A', '--', *paths) unless paths.empty?
      tree = git.call('write-tree')
      base_commit = git.call('rev-parse', '--verify', "#{base}^{commit}")
      changed = RollingRelease.git_diff_paths(root, base_commit, tree)
      RollingRelease.validate_source_compatibility(root, base_commit, tree, changed) unless changed.empty?
    end
  end
end

if $PROGRAM_NAME == __FILE__
  begin
    WorkingCompatibility.check(Dir.pwd, ARGV.fetch(0))
    puts '작업 트리 배포 소스 호환성 통과 (운영 기준 비교는 CI에서 재검증)'
  rescue StandardError => error
    warn error.message
    exit 1
  end
end
