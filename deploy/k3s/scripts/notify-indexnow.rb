#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'fileutils'
require 'json'
require 'net/http'
require 'tempfile'
require 'timeout'

module IndexNow
  class Error < StandardError; end

  HOST = 'happy-gallery.com'
  ORIGIN = "https://#{HOST}"
  ENDPOINT = URI('https://api.indexnow.org/indexnow')
  SOURCES = %w[products classes events notices].freeze
  BATCH_SIZE = 10_000
  PAGE_PATH = %r{\A/(?:|(?:products|classes|events)(?:/[1-9][0-9]*)?|notices/[1-9][0-9]*)\z}

  def self.request(uri, body = nil)
    http = Net::HTTP.new(uri.host, uri.port, nil)
    http.use_ssl = true
    http.verify_mode = OpenSSL::SSL::VERIFY_PEER
    http.min_version = OpenSSL::SSL::TLS1_2_VERSION
    http.open_timeout = 3
    http.read_timeout = 10
    http.write_timeout = 10
    http.max_retries = 0
    request = body ? Net::HTTP::Post.new(uri) : Net::HTTP::Get.new(uri)
    request['Cache-Control'] = 'no-cache'
    if body
      request['Content-Type'] = 'application/json; charset=utf-8'
      request.body = JSON.generate(body)
    end
    Timeout.timeout(20) do
      http.start do |connection|
        connection.request(request) do |response|
          content = +''
          response.read_body do |chunk|
            content << chunk
            raise Error, 'HTTP 응답이 16MiB를 초과했습니다.' if content.bytesize > 16 * 1024 * 1024
          end
          return [response.code.to_i, content]
        end
      end
    end
  end

  def self.canonical(value)
    case value
    when Hash then value.keys.sort.to_h { |key| [key, canonical(value[key])] }
    when Array then value.map { |item| canonical(item) }
    else value
    end
  end

  def self.fingerprint(value)
    Digest::SHA256.hexdigest(JSON.generate(canonical(value)))
  end

  def self.snapshot
    pages = {}
    catalogs = SOURCES.to_h do |source|
      status, body = request(URI("#{ORIGIN}/api/v1/#{source}"))
      raise Error, "#{source} 조회 실패 (HTTP #{status})." unless status == 200
      rows = JSON.parse(body)
      raise Error, "#{source} 목록 형식이 올바르지 않습니다." unless rows.is_a?(Array)
      entries = rows.to_h do |row|
        raise Error, "#{source} 항목 형식이 올바르지 않습니다." unless
          row.is_a?(Hash) && row['id'].is_a?(Integer) && row['id'].positive?
        # 공지 본문 변경은 version으로 감지한다. 상세 조회는 조회수를 올리므로 호출하지 않는다.
        if source == 'notices'
          raise Error, '공지 version이 없습니다.' unless row['version'].is_a?(Integer)
          row = row.reject { |key, _value| key == 'viewCount' }
        end
        ["/#{source}/#{row.fetch('id')}", fingerprint(row)]
      end
      raise Error, "#{source}에 중복 ID가 있습니다." unless entries.size == rows.size
      pages.merge!(entries)
      pages["/#{source}"] = fingerprint(entries) unless source == 'notices'
      [source, entries]
    end
    pages['/'] = fingerprint(catalogs)
    pages
  rescue JSON::ParserError
    raise Error, '공개 API가 JSON 목록을 반환하지 않았습니다.'
  end

  def self.read_state(path)
    return nil unless File.exist?(path)
    state = JSON.parse(File.read(path))
    pages = state['pages'] if state.is_a?(Hash) && state['version'] == 1
    raise Error, 'IndexNow 상태 파일 형식이 올바르지 않습니다.' unless
      pages.is_a?(Hash) && pages.all? do |url, digest|
        PAGE_PATH.match?(url) && digest.is_a?(String) && /\A[a-f0-9]{64}\z/.match?(digest)
      end
    pages
  rescue JSON::ParserError
    raise Error, 'IndexNow 상태 파일을 읽을 수 없습니다.'
  end

  def self.write_state(path, pages)
    Tempfile.create(['indexnow-', '.json'], File.dirname(path)) do |file|
      file.write(JSON.generate('version' => 1, 'pages' => pages))
      file.flush
      file.fsync
      File.rename(file.path, path)
    end
  end

  def self.sync(directory, key:)
    raise Error, 'INDEXNOW_KEY는 영문·숫자·하이픈 8~128자여야 합니다.' unless
      /\A[a-zA-Z0-9-]{8,128}\z/.match?(key)
    FileUtils.mkdir_p(directory, mode: 0o700)
    File.open(File.join(directory, 'sync.lock'), File::RDWR | File::CREAT, 0o600) do |lock|
      raise Error, '다른 IndexNow 작업이 실행 중입니다.' unless lock.flock(File::LOCK_EX | File::LOCK_NB)
      path = File.join(directory, 'pages.json')
      previous = read_state(path)
      current = snapshot
      changed = previous ? (previous.keys | current.keys).sort.select { |url| previous[url] != current[url] } : []
      if previous && changed.empty?
        puts '[indexnow] 변경 없음'
        return
      end

      status, body = request(URI("#{ORIGIN}/#{key}.txt"))
      raise Error, '공개 키 파일을 확인할 수 없습니다. 프런트엔드 설정과 배포를 확인하세요.' unless
        status == 200 && body.strip == key
      unless previous
        write_state(path, current)
        puts "[indexnow] 비교 기준 저장: #{current.size}개 URL, 외부 제출 없음"
        return
      end

      changed.each_slice(BATCH_SIZE) do |batch|
        status, = request(ENDPOINT, 'host' => HOST, 'key' => key,
                                    'keyLocation' => "#{ORIGIN}/#{key}.txt",
                                    'urlList' => batch.map { |url| "#{ORIGIN}#{url}" })
        raise Error, "IndexNow 제출 실패 (HTTP #{status}). 다음 실행에서 재시도합니다." unless [200, 202].include?(status)
        # 접수된 묶음만 저장해 다음 묶음 실패 시 이미 접수된 URL을 다시 보내지 않는다.
        batch.each { |url| current.key?(url) ? previous[url] = current[url] : previous.delete(url) }
        write_state(path, previous)
        puts "[indexnow] #{batch.size}개 URL 접수 (HTTP #{status}, 검색 반영 여부는 검색엔진에서 결정)"
      end
    end
  end
end

if $PROGRAM_NAME == __FILE__
  begin
    enabled = ENV.fetch('INDEXNOW_ENABLED', 'false')
    raise IndexNow::Error, 'INDEXNOW_ENABLED는 true 또는 false여야 합니다.' unless %w[true false].include?(enabled)
    if enabled == 'false'
      puts '[indexnow] 비활성'
    else
      raise IndexNow::Error, '사용법: notify-indexnow.rb <상태 디렉터리>' unless ARGV.size == 1
      IndexNow.sync(ARGV.fetch(0), key: ENV.fetch('INDEXNOW_KEY', ''))
    end
  rescue IndexNow::Error => error
    warn "[indexnow] #{error.message}"
    exit 1
  rescue StandardError => error
    warn "[indexnow] 실행 실패 (#{error.class}). 기존 비교 기준을 유지합니다."
    exit 1
  end
end
