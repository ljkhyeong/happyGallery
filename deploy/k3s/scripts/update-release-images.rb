#!/usr/bin/env ruby
# frozen_string_literal: true

require 'tempfile'

def atomic_write(path, contents, owner)
  Tempfile.create(['.release-', '.env'], File.dirname(path)) do |file|
    file.chmod(owner.mode & 0o777)
    file.chown(owner.uid, owner.gid) if Process.uid.zero?
    file.write(contents)
    file.flush
    file.fsync
    File.rename(file.path, path)
  end
end

begin
  check_only = ARGV.first == '--check'
  ARGV.shift if check_only
  expected_count = check_only ? 1 : 4
  abort '사용법: update-release-images.rb [--check] <release.env> [tag app-digest frontend-digest]' unless ARGV.size == expected_count
  path = File.realpath(ARGV.fetch(0))
  owner = File.stat(path)
  abort 'release.env는 일반 파일이어야 합니다.' unless owner.file?
  abort 'release.env와 상위 디렉터리에 쓰기 권한이 필요합니다.' unless File.writable?(path) && File.writable?(File.dirname(path))
  abort 'release.env 권한을 600으로 제한하세요.' unless (owner.mode & 0o077).zero?

  File.open("#{path}.lock", File::RDWR | File::CREAT, 0o600) do |lock|
    abort '다른 release.env 갱신이 진행 중입니다.' unless lock.flock(File::LOCK_EX | File::LOCK_NB)
    original = File.read(path)
    keys = {}
    original.each_line.with_index(1) do |line, number|
      next if line.strip.empty? || line.lstrip.start_with?('#')
      match = /\A([A-Z][A-Z0-9_]*)=([^\r\n]*)\n?\z/.match(line)
      abort "release.env #{number}행 형식이 올바르지 않습니다." unless match
      key = match[1]
      abort "중복된 환경 변수: #{key}" if keys.key?(key)
      keys[key] = true
    end
    if check_only
      puts 'release.env 자동 갱신 준비 OK'
      exit
    end

    tag, app_digest, frontend_digest = ARGV.drop(1)
    abort 'IMAGE_TAG는 40자리 Git SHA여야 합니다.' unless tag.match?(/\A[a-f0-9]{40}\z/)
    abort '이미지 digest 형식이 올바르지 않습니다.' unless [app_digest, frontend_digest].all? { |digest| digest.match?(/\Asha256:[a-f0-9]{64}\z/) }
    values = { 'IMAGE_TAG' => tag,
               'APP_IMAGE' => "localhost/happygallery-app:#{tag}",
               'FRONTEND_IMAGE' => "localhost/happygallery-frontend:#{tag}",
               'APP_IMAGE_DIGEST' => app_digest, 'FRONTEND_IMAGE_DIGEST' => frontend_digest }
    updated = original.lines.map do |line|
      key = line.split('=', 2).first
      values.key?(key) ? "#{key}=#{values.fetch(key)}\n" : line
    end.join
    updated += "\n" unless updated.empty? || updated.end_with?("\n")
    values.each { |key, value| updated += "#{key}=#{value}\n" unless keys.key?(key) }
    if updated != original
      atomic_write("#{path}.previous", original, owner)
      atomic_write(path, updated, owner)
    end
    puts "이미지 설정 자동 갱신 완료: #{path}"
  end
rescue SystemCallError, IOError => error
  warn "release.env 갱신 실패: #{error.class}"
  exit 1
end
