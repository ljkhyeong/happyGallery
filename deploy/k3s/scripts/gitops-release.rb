# frozen_string_literal: true

require 'json'
require 'yaml'
require 'digest'
require 'fileutils'
require 'open3'

module GitopsRelease
  KEYS = %w[source_sha app_digest frontend_digest acme_email manifest_sha256].freeze
  BRANCH = 'gitops-production'

  def self.canonical(value)
    case value
    when Hash then value.keys.sort.to_h { |key| [key, canonical(value.fetch(key))] }
    when Array then value.map { |entry| canonical(entry) }
    else value
    end
  end

  def self.manifest_digest(path)
    documents = YAML.load_stream(File.read(path)).compact.sort_by do |document|
      [document.fetch('apiVersion'), document.fetch('kind'),
       document.dig('metadata', 'namespace').to_s, document.dig('metadata', 'name')]
    end
    Digest::SHA256.hexdigest(JSON.generate(canonical(documents)))
  end

  def self.validate(release)
    raise 'GitOps release 필드가 다릅니다.' unless release.keys.sort == KEYS.sort
    raise '40자리 소스 SHA가 필요합니다.' unless release['source_sha'].to_s.match?(/\A[a-f0-9]{40}\z/)
    %w[app_digest frontend_digest].each do |key|
      raise "잘못된 #{key}" unless release[key].to_s.match?(/\Asha256:[a-f0-9]{64}\z/)
    end
    raise '잘못된 manifest 해시' unless release['manifest_sha256'].to_s.match?(/\A[a-f0-9]{64}\z/)
    raise '잘못된 ACME 이메일' unless release['acme_email'].to_s.match?(/\A[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\z/)
    release
  end

  def self.hook(phase, release)
    script = <<~'JS'
      const net = require('node:net');
      const request = JSON.parse(process.env.GITOPS_REQUEST);
      const client = net.createConnection('/control/control.sock');
      let response = '';
      client.setTimeout(3200000, () => client.destroy(new Error('호스트 응답 시간이 초과됐습니다.')));
      client.on('connect', () => client.write(JSON.stringify(request) + '\n'));
      client.on('data', chunk => { response += chunk; });
      client.on('end', () => {
        try {
          const result = JSON.parse(response);
          console.log(result.message);
          process.exitCode = result.ok ? 0 : 1;
        } catch (error) { console.error(error.message); process.exitCode = 1; }
      });
      client.on('error', error => { console.error(error.message); process.exitCode = 1; });
    JS
    {
      'apiVersion' => 'batch/v1', 'kind' => 'Job',
      'metadata' => {
        'name' => "happygallery-gitops-#{phase}", 'namespace' => 'happygallery',
        'annotations' => {
          'argocd.argoproj.io/hook' => { 'prepare' => 'PreSync', 'complete' => 'PostSync', 'failed' => 'SyncFail' }.fetch(phase),
          'argocd.argoproj.io/hook-delete-policy' => 'BeforeHookCreation,HookSucceeded'
        }
      },
      'spec' => {
        'backoffLimit' => 0, 'activeDeadlineSeconds' => 3300,
        'template' => { 'spec' => {
          'restartPolicy' => 'Never', 'automountServiceAccountToken' => false,
          'securityContext' => { 'runAsNonRoot' => true, 'runAsUser' => 101, 'runAsGroup' => 101,
                                 'seccompProfile' => { 'type' => 'RuntimeDefault' } },
          'containers' => [{
            'name' => 'control',
            'image' => "ghcr.io/ljkhyeong/happygallery-frontend@#{release.fetch('frontend_digest')}",
            'imagePullPolicy' => 'IfNotPresent', 'command' => ['node', '-e', script],
            'env' => [{ 'name' => 'GITOPS_REQUEST', 'value' => JSON.generate(release.merge('phase' => phase)) }],
            'resources' => { 'requests' => { 'cpu' => '10m', 'memory' => '32Mi' },
                             'limits' => { 'cpu' => '100m', 'memory' => '64Mi' } },
            'securityContext' => { 'allowPrivilegeEscalation' => false, 'readOnlyRootFilesystem' => true,
                                   'capabilities' => { 'drop' => ['ALL'] } },
            'volumeMounts' => [{ 'name' => 'control', 'mountPath' => '/control', 'readOnly' => true }]
          }],
          'volumes' => [{ 'name' => 'control', 'hostPath' => { 'path' => '/run/happygallery-gitops', 'type' => 'Directory' } }]
        } }
      }
    }
  end

  def self.generate(output, environment = ENV)
    release = {
      'source_sha' => environment.fetch('GITHUB_SHA'), 'app_digest' => environment.fetch('APP_REGISTRY_DIGEST'),
      'frontend_digest' => environment.fetch('FRONTEND_REGISTRY_DIGEST'),
      'acme_email' => environment.fetch('ACME_EMAIL'), 'manifest_sha256' => '0' * 64
    }
    validate(release)
    render_env = {
      'PUBLIC_HOST' => 'happy-gallery.com', 'ACME_EMAIL' => release['acme_email'],
      'APP_IMAGE' => "localhost/happygallery-app:#{release['source_sha']}",
      'FRONTEND_IMAGE' => "localhost/happygallery-frontend:#{release['source_sha']}",
      'APP_IMAGE_DIGEST' => release['app_digest'], 'FRONTEND_IMAGE_DIGEST' => release['frontend_digest'],
      'IMAGE_TAG' => release['source_sha']
    }
    FileUtils.mkdir_p(output)
    manifest = File.join(output, 'manifests.yaml')
    raise 'manifest 렌더링 실패' unless system(render_env, File.join(__dir__, 'render-manifests.sh'), manifest)
    release['manifest_sha256'] = manifest_digest(manifest)
    documents = YAML.load_stream(File.read(manifest)).compact
    documents.each do |document|
      annotations = (document.fetch('metadata')['annotations'] ||= {})
      annotations['argocd.argoproj.io/sync-options'] = 'Prune=false,Delete=false' if
        %w[Namespace PersistentVolumeClaim StorageClass ClusterIssuer].include?(document['kind']) || document['kind'] == 'StatefulSet'
      wave = case document.dig('metadata', 'name')
             when 'app' then 0
             when 'frontend' then 5
             else -10
             end
      annotations['argocd.argoproj.io/sync-wave'] = wave.to_s
    end
    File.write(manifest, documents.map { |document| YAML.dump(document) }.join)
    File.write(File.join(output, 'hooks.yaml'), %w[prepare complete failed].map { |phase| YAML.dump(hook(phase, release)) }.join)
    File.write(File.join(output, 'release.json'), JSON.pretty_generate(release) + "\n")
    release
  end
end

if $PROGRAM_NAME == __FILE__
  begin
    GitopsRelease.generate(ARGV.fetch(0))
  rescue StandardError => error
    abort "오류: #{error.message}"
  end
end
