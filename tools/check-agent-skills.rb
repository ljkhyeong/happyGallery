#!/usr/bin/env ruby
# 프로젝트 스킬의 형식·파일명·참조를 외부 패키지 설치 없이 검사한다.
require 'yaml'
require 'pathname'
require 'open3'
require 'uri'

root = Pathname.new(ARGV.fetch(0, File.expand_path('..', __dir__))).expand_path
skills = root.join('.agents/skills')
abort '스킬 폴더가 없습니다: .agents/skills' unless skills.directory?

errors = []
folders = skills.children.select(&:directory?).sort
errors << '검사할 스킬이 없습니다.' if folders.empty?

folders.each do |folder|
  name = folder.basename.to_s
  entries = folder.children.map { |path| path.basename.to_s }.select { |file| file.downcase == 'skill.md' }
  unless entries == ['SKILL.md']
    errors << "#{name}: 진입 파일은 SKILL.md 하나여야 합니다."
    next
  end

  text = folder.join('SKILL.md').read
  frontmatter = text.match(/\A---\r?\n(.*?)\r?\n---(?:\r?\n|\z)/m)
  unless frontmatter
    errors << "#{name}: YAML 머리말이 없습니다."
    next
  end

  begin
    data = YAML.safe_load(frontmatter[1])
    unless data.is_a?(Hash) && data['name'] == name && name.match?(/\A[a-z0-9]+(?:-[a-z0-9]+)*\z/)
      errors << "#{name}: name은 스킬 폴더명과 같은 소문자·숫자·하이픈 이름이어야 합니다."
    end
    description = data.is_a?(Hash) ? data['description'] : nil
    unless description.is_a?(String) && !description.strip.empty? && description.length <= 1024
      errors << "#{name}: description은 1~1024자의 문자열이어야 합니다."
    end

    metadata = folder.join('agents/openai.yaml')
    if metadata.file?
      ui = YAML.safe_load(metadata.read)
      interface = ui.is_a?(Hash) ? ui['interface'] : nil
      prompt = interface.is_a?(Hash) ? interface['default_prompt'] : nil
      if prompt && (!prompt.is_a?(String) || !prompt.include?("$#{name}"))
        errors << "#{name}: UI default_prompt가 해당 스킬을 참조해야 합니다."
      end
    end
  rescue Psych::Exception => error
    errors << "#{name}: YAML 오류 (#{error.class})"
  end

  folder.glob('**/*.md').each do |document|
    document.read.scan(/\]\(([^\s)]+)\)/).flatten.each do |link|
      next if link.start_with?('#') || link.match?(/\A[a-z][a-z0-9+.-]*:/i)
      path = URI::DEFAULT_PARSER.unescape(link.split('#', 2).first)
      errors << "#{name}: 없는 참조 #{link}" unless document.dirname.join(path).exist?
    end
  end
end

# macOS 파일 조회만으로는 Git에 남은 소문자 진입 파일을 구분할 수 없다.
index, _stderr, status = Open3.capture3('git', '-C', root.to_s, 'ls-files', '-z', '--', '.agents/skills')
if status.success?
  index.split("\0").each do |path|
    file = File.basename(path)
    errors << "Git 파일명을 SKILL.md로 변경하세요: #{path}" if file.downcase == 'skill.md' && file != 'SKILL.md'
  end
end

abort errors.join("\n") unless errors.empty?
puts "스킬 #{folders.length}개 검사 통과: YAML·이름·참조·Git 파일명"
