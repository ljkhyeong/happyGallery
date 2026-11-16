#!/usr/bin/env ruby
# frozen_string_literal: true

require 'timeout'
require_relative 'gitops-release'

module GitopsControl
  REPOSITORY = '/opt/happygallery'

  def self.git(*arguments)
    output, status = Open3.capture2e('git', '-C', REPOSITORY, *arguments)
    raise "GitOps 소스 조회 실패: #{output}" unless status.success?
    output.strip
  end

  def self.request(input)
    value = JSON.parse(input)
    raise '허용되지 않은 배포 단계입니다.' unless %w[prepare complete failed].include?(value['phase'])
    release = value.reject { |key, _| key == 'phase' }
    GitopsRelease.validate(release)
    [value.fetch('phase'), release]
  end

  def self.run(script, *arguments, environment: {}, log:)
    success = system(environment, 'bash', script, *arguments, out: log, err: log)
    raise "배포 단계 실패. 서버 로그를 확인하세요: #{log.path}" unless success
  end

  def self.handle(phase, release)
    state = File.join(ENV.fetch('HOME'), '.local/state/happygallery')
    cd_state = File.join(state, 'cd')
    releases = File.join(state, 'releases')
    FileUtils.mkdir_p(cd_state)
    environment = {
      'HAPPYGALLERY_RELEASE_DIR' => releases, 'KUBECTL_BIN' => '/usr/local/libexec/happygallery-kubectl',
      'GIT_TERMINAL_PROMPT' => '0', 'GITOPS_MANIFEST_SHA256' => release.fetch('manifest_sha256'),
      'HAPPYGALLERY_ONLINE_BACKUP_BOOTSTRAP' => nil, 'VERIFIED_RECOVERY_BUNDLE_OVERRIDE' => nil
    }
    File.open(File.join(cd_state, '.lock'), File::RDWR | File::CREAT, 0o600) do |lock|
      raise '다른 배포가 진행 중입니다.' unless lock.flock(File::LOCK_EX | File::LOCK_NB)
      record = File.join(cd_state, 'gitops-request.json')
      active = File.file?(File.join(releases, 'gitops-active'))
      previous = JSON.parse(File.read(record)) if File.file?(record) && (active || phase != 'prepare')
      raise '진행 중인 다른 release를 먼저 완료하세요.' if active && previous != release
      checkout = File.join(cd_state, 'gitops-source')
      if phase == 'prepare' && !active
        raise 'origin 저장소가 다릅니다.' unless git('remote', 'get-url', 'origin') == 'https://github.com/ljkhyeong/happyGallery.git'
        git('fetch', '--no-tags', 'origin',
            '+refs/heads/main:refs/remotes/happygallery-gitops/main',
            "+refs/heads/#{GitopsRelease::BRANCH}:refs/remotes/happygallery-gitops/desired")
        desired = JSON.parse(git('show', 'refs/remotes/happygallery-gitops/desired:production/release.json'))
        raise 'Git의 배포 요청과 다릅니다.' unless GitopsRelease.validate(desired) == release
        git('merge-base', '--is-ancestor', release.fetch('source_sha'), 'refs/remotes/happygallery-gitops/main')
        if File.directory?(checkout)
          raise 'GitOps checkout이 수정됐습니다.' unless git('-C', checkout, 'status', '--porcelain').empty?
          git('-C', checkout, 'checkout', '--detach', release.fetch('source_sha'))
        else
          git('worktree', 'add', '--detach', checkout, release.fetch('source_sha'))
        end
        File.write("#{record}.tmp", JSON.generate(release), mode: 'w', perm: 0o600)
        File.rename("#{record}.tmp", record)
      else
        raise '준비한 요청 기록이 없습니다.' unless previous == release
      end
      raise '배포 소스가 요청과 다릅니다.' unless git('-C', checkout, 'rev-parse', 'HEAD') == release.fetch('source_sha')
      raise 'GitOps checkout이 수정됐습니다.' unless git('-C', checkout, 'status', '--porcelain').empty?
      scripts = File.join(checkout, 'deploy/k3s/scripts')
      release_env = '/etc/happygallery/release.env'
      File.open(File.join(cd_state, 'gitops.log'), 'a', 0o600) do |log|
        log.puts "#{Time.now.utc} #{phase} #{release.fetch('source_sha')}"
        log.flush
        if phase == 'prepare' && !active
          run(File.join(scripts, 'deploy-registry-images.sh'), release_env, release.fetch('source_sha'),
              release.fetch('app_digest'), release.fetch('frontend_digest'),
              environment: environment.merge('HAPPYGALLERY_GITOPS_PHASE' => 'prepare'), log: log)
        elsif active
          FileUtils.mkdir_p(releases)
          File.open(File.join(releases, '.deploy.lock'), File::RDWR | File::CREAT, 0o600) do |deploy_lock|
            raise '다른 배포가 진행 중입니다.' unless deploy_lock.flock(File::LOCK_EX | File::LOCK_NB)
            run(File.join(scripts, 'rollout.sh'), release_env, phase == 'prepare' ? 'resume' : phase,
                environment: environment, log: log)
          end
        elsif phase != 'failed'
          # 완료 응답 유실 뒤 재시도도 실제 검증을 다시 수행한다.
          metadata = File.readlines(File.join(releases, 'current/metadata.env'), chomp: true).to_h { |line| line.split('=', 2) }
          expected = { 'IMAGE_TAG' => release.fetch('source_sha'), 'APP_IMAGE_DIGEST' => release.fetch('app_digest'),
                       'FRONTEND_IMAGE_DIGEST' => release.fetch('frontend_digest') }
          raise '완료한 release 기록이 다릅니다.' unless metadata.slice(*expected.keys) == expected
          %w[app frontend].each do |component|
            image, status = Open3.capture2e(environment, environment.fetch('KUBECTL_BIN'), '-n', 'happygallery',
                                           'get', 'deployment', component, '-o', 'jsonpath={.spec.template.spec.containers[0].image}')
            expected_image = "localhost/happygallery-#{component}:#{release.fetch('source_sha')}@#{release.fetch("#{component}_digest")}"
            raise "#{component} 실제 이미지가 완료한 release와 다릅니다." unless status.success? && image == expected_image
          end
          run(File.join(scripts, 'verify.sh'), 'happy-gallery.com', environment: environment, log: log)
        end
      end
    end
  end
end

if $PROGRAM_NAME == __FILE__
  File.umask(0o077)
  begin
    input = Timeout.timeout(10) { STDIN.gets(4097) }
    raise '잘못된 요청 크기입니다.' unless input && input.bytesize <= 4096 && input.end_with?("\n")
    phase, release = GitopsControl.request(input)
    GitopsControl.handle(phase, release)
    puts JSON.generate('ok' => true, 'message' => "#{phase} 완료: #{release.fetch('source_sha')}")
  rescue StandardError => error
    puts JSON.generate('ok' => false, 'message' => error.message)
  end
end
