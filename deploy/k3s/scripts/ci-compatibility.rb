# frozen_string_literal: true

require_relative 'rolling-release'

# GitOps 전환 후 CI는 마지막 승격 소스와 비교한다. 실제 운영 기준 검사는 PreSync가 수행한다.
module CiCompatibility
  def self.api(path)
    output, status = Open3.capture2e('gh', 'api', '--paginate', '--slurp', path)
    raise '배포 이력 조회에 실패했습니다. GitHub actions:read 권한을 확인하세요.' unless status.success?
    JSON.parse(output)
  end

  def self.baseline(event, production:, repository:, api: method(:api), published_source: nil)
    return event.fetch('pull_request').fetch('base').fetch('sha') if event.key?('pull_request')
    if production && published_source
      raise 'GitOps 배포 소스 SHA가 올바르지 않습니다.' unless published_source.match?(/\A[a-f0-9]{40}\z/)
      return published_source
    end
    return event['before'] if !production && event['before'].to_s.match?(/\A[a-f0-9]{40}\z/) && event['before'] != '0' * 40

    runs = api.call("repos/#{repository}/actions/workflows/production.yml/runs?branch=main&status=success&per_page=100")
              .flat_map { |page| page.fetch('workflow_runs') }
              .sort_by { |run| run.fetch('updated_at') }.reverse
    runs.each do |run|
      jobs = api.call("repos/#{repository}/actions/runs/#{run.fetch('id')}/jobs?per_page=100")
                .flat_map { |page| page.fetch('jobs') }
      next unless jobs.any? { |job| job['name'] == 'Roll out production' && job['conclusion'] == 'success' }

      return run.fetch('head_sha')
    end
    raise '성공한 운영 배포 이력이 없습니다. 최초 배포 기준을 별도로 확인하세요.'
  end
end

if $PROGRAM_NAME == __FILE__
  begin
    event = JSON.parse(File.read(ENV.fetch('GITHUB_EVENT_PATH')))
    published_source = nil
    if ENV['PRODUCTION_RELEASE'] == 'true' && !event.key?('pull_request')
      _reference, reference_status = Open3.capture2e('git', 'show-ref', '--verify', '--quiet', 'refs/remotes/origin/gitops-production')
      if reference_status.success?
        desired, desired_status = Open3.capture2e('git', 'show', 'refs/remotes/origin/gitops-production:production/release.json')
        raise 'GitOps 배포 기록을 읽지 못했습니다.' unless desired_status.success?
        published_source = JSON.parse(desired).fetch('source_sha')
      end
    end
    base = CiCompatibility.baseline(event, production: ENV['PRODUCTION_RELEASE'] == 'true',
                                   repository: ENV.fetch('GITHUB_REPOSITORY'), published_source: published_source)
    head = ENV.fetch('GITHUB_SHA')
    reset_id = RollingRelease.check_source(Dir.pwd, base, head)
    puts "배포 소스 호환성 통과: #{base} → #{head}"
    puts "스키마 초기화 release(#{reset_id}): 서버 배포에 HAPPYGALLERY_SCHEMA_RESET 확인이 필요합니다." if reset_id
  rescue StandardError => error
    abort "오류: #{error.message}"
  end
end
