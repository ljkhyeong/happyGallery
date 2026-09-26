# frozen_string_literal: true

require_relative 'rolling-release'

# 실패한 main push가 이어져도 마지막 실제 배포 이후의 변경을 모두 검사한다.
module CiCompatibility
  def self.api(path)
    output, status = Open3.capture2e('gh', 'api', '--paginate', '--slurp', path)
    raise '배포 이력 조회에 실패했습니다. GitHub actions:read 권한을 확인하세요.' unless status.success?
    JSON.parse(output)
  end

  def self.baseline(event, production:, repository:, api: method(:api))
    return event.fetch('pull_request').fetch('base').fetch('sha') if event.key?('pull_request')
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
    base = CiCompatibility.baseline(event, production: ENV['PRODUCTION_RELEASE'] == 'true',
                                   repository: ENV.fetch('GITHUB_REPOSITORY'))
    head = ENV.fetch('GITHUB_SHA')
    RollingRelease.check_source(Dir.pwd, base, head)
    puts "배포 소스 호환성 통과: #{base} → #{head}"
  rescue StandardError => error
    abort "오류: #{error.message}"
  end
end
