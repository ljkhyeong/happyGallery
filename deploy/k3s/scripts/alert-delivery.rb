#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'yaml'
require 'uri'

# 운영 환경 파일은 실행하지 않고 읽는다. 오류에는 행 내용이나 자격 증명을 담지 않는다.
module AlertDelivery
  class ConfigError < StandardError; end
  SECRET_PATH = '/etc/alertmanager/secrets'
  EMAIL = /\A[A-Za-z0-9._%+\-]+@[A-Za-z0-9](?:[A-Za-z0-9.\-]*[A-Za-z0-9])?\.[A-Za-z]{2,}\z/

  def self.private_contents(path)
    stat = File.stat(path)
    raise ConfigError, '입력 파일은 일반 파일이며 권한이 600이어야 합니다.' unless
      stat.file? && (stat.mode & 0o077).zero?

    File.read(path)
  end

  def self.parse_env(contents)
    contents.lines.each_with_index.each_with_object({}) do |(line, index), values|
      next if line.strip.empty? || line.lstrip.start_with?('#')

      match = /\A([A-Z][A-Z0-9_]*)=([^\r\n]*)\n?\z/.match(line)
      raise ConfigError, "환경 파일 #{index + 1}행 형식이 올바르지 않습니다." unless match
      key, value = match.captures
      raise ConfigError, "중복된 환경 변수: #{key}" if values.key?(key)

      values[key] = value
    end
  end

  def self.required(values, key)
    value = values[key]
    raise ConfigError, "#{key} 값이 필요합니다." if value.nil? || value.empty?
    value
  end

  def self.email_address(values, key)
    value = required(values, key)
    raise ConfigError, "#{key}에는 따옴표 없이 이메일 주소 한 개를 입력하세요." unless EMAIL.match?(value)
    value
  end

  def self.smtp_settings(app_file, alert_file)
    alerts = parse_env(private_contents(alert_file))
    unknown = alerts.keys - ['ALERT_EMAIL_TO']
    raise ConfigError, "알림 환경 파일의 허용되지 않은 키: #{unknown.join(', ')}" unless unknown.empty?
    app = parse_env(private_contents(app_file))
    raise ConfigError, '장애 메일에는 EMAIL_VERIFICATION_PROVIDER=smtp가 필요합니다.' unless
      app.fetch('EMAIL_VERIFICATION_PROVIDER', 'smtp') == 'smtp'
    raise ConfigError, '장애 메일에는 STARTTLS=true, SSL=false가 필요합니다.' unless
      app.fetch('EMAIL_VERIFICATION_STARTTLS_ENABLED', 'true') == 'true' &&
      app.fetch('EMAIL_VERIFICATION_SSL_ENABLED', 'false') == 'false'
    host = required(app, 'EMAIL_VERIFICATION_SMTP_HOST')
    raise ConfigError, 'SMTP 호스트에는 DNS 이름을 입력하세요.' unless
      /\A[A-Za-z0-9](?:[A-Za-z0-9.\-]*[A-Za-z0-9])?\z/.match?(host)
    port = app.fetch('EMAIL_VERIFICATION_SMTP_PORT', '587')
    raise ConfigError, 'STARTTLS SMTP 포트는 25, 587, 2587 중 하나여야 합니다.' unless %w[25 587 2587].include?(port)

    {
      'host' => host, 'port' => port.to_i,
      'from' => email_address(app, 'EMAIL_VERIFICATION_FROM'),
      'to' => email_address(alerts, 'ALERT_EMAIL_TO'),
      'username' => required(app, 'EMAIL_VERIFICATION_SMTP_USERNAME'),
      'password' => required(app, 'EMAIL_VERIFICATION_SMTP_PASSWORD')
    }
  end

  def self.render(app_file, alert_file, output_dir)
    config = YAML.safe_load(File.read(File.expand_path('../alertmanager.yml', __dir__)))
    input = private_contents(alert_file)
    files = {}
    if input.start_with?('https://')
      url = input.strip
      uri = URI.parse(url)
      raise ConfigError, '웹훅 파일에는 실제 HTTPS URL 한 줄만 입력하세요.' unless
        uri.is_a?(URI::HTTPS) && uri.host && !url.match?(/\s/) &&
        !uri.userinfo && !uri.fragment && !url.include?('example.com')
      files['webhook-url'] = url
    else
      smtp = smtp_settings(app_file, alert_file)
      config['global'].merge!(
        'smtp_smarthost' => "#{smtp['host']}:#{smtp['port']}",
        'smtp_from' => smtp['from'],
        'smtp_auth_username' => smtp['username'],
        'smtp_auth_password_file' => "#{SECRET_PATH}/smtp-password",
        'smtp_require_tls' => true,
        'smtp_tls_config' => { 'server_name' => smtp['host'], 'min_version' => 'TLS12' }
      )
      ([config['route']] + config['route'].fetch('routes')).each do |route|
        route['receiver'] = route.fetch('receiver').sub('webhook-', 'email-')
      end
      config.fetch('receivers').each do |receiver|
        receiver['name'] = receiver.fetch('name').sub('webhook-', 'email-')
        receiver.delete('webhook_configs')
        receiver['email_configs'] = [{ 'to' => smtp['to'], 'send_resolved' => true }]
      end
      files['smtp-password'] = smtp['password']
    end
    # JSON은 YAML의 부분 집합이다. 문자열을 직접 치환하지 않아 특수문자를 보존한다.
    files['alertmanager.yml'] = JSON.pretty_generate(config) + "\n"
    stat = File.stat(output_dir)
    raise ConfigError, '출력 디렉터리는 비어 있고 권한이 700이어야 합니다.' unless
      stat.directory? && (stat.mode & 0o077).zero? && Dir.children(output_dir).empty?
    files.each do |name, contents|
      File.open(File.join(output_dir, name), File::WRONLY | File::CREAT | File::EXCL, 0o600) do |file|
        file.write(contents)
      end
    end
  end

  def self.send_email(app_file, alert_file, subject, body)
    require 'net/smtp'
    require 'openssl'
    require 'timeout'
    require 'time'
    smtp = smtp_settings(app_file, alert_file)
    raise ConfigError, '메일 제목에 줄바꿈을 넣을 수 없습니다.' if subject.match?(/[\r\n]/)
    encoded_subject = [subject.encode('UTF-8')].pack('m0')
    message = [
      "From: #{smtp['from']}", "To: #{smtp['to']}",
      "Date: #{Time.now.rfc2822}", "Subject: =?UTF-8?B?#{encoded_subject}?=",
      'MIME-Version: 1.0', 'Content-Type: text/plain; charset=UTF-8',
      'Content-Transfer-Encoding: base64', '', [body.encode('UTF-8')].pack('m')
    ].join("\r\n")
    client = Net::SMTP.new(smtp['host'], smtp['port'])
    client.open_timeout = 3
    client.read_timeout = 10
    context = OpenSSL::SSL::SSLContext.new
    context.set_params(min_version: OpenSSL::SSL::TLS1_2_VERSION)
    client.enable_starttls(context)
    Timeout.timeout(20) do
      client.start('happy-gallery.com', smtp['username'], smtp['password'], :plain) do |session|
        session.send_message(message, smtp['from'], smtp['to'])
      end
    end
  end
end

if $PROGRAM_NAME == __FILE__
  begin
    command = ARGV.shift
    case command
    when 'render'
      raise AlertDelivery::ConfigError, '사용법: alert-delivery.rb render <app.env> <alertmanager.env|webhook-url> <빈 출력 디렉터리>' unless ARGV.size == 3
      AlertDelivery.render(*ARGV)
    when 'send'
      raise AlertDelivery::ConfigError, '사용법: alert-delivery.rb send <app.env> <alertmanager.env> <제목> <본문>' unless ARGV.size == 4
      AlertDelivery.send_email(*ARGV)
      puts '장애 알림 메일을 SMTP 서버에 접수했습니다. 수신함에서 도착을 확인하세요.'
    else
      raise AlertDelivery::ConfigError, 'render 또는 send 명령을 지정하세요.'
    end
  rescue AlertDelivery::ConfigError => error
    abort "오류: #{error.message}"
  rescue StandardError => error
    # SMTP 제공자 응답에는 주소 등 개인정보가 포함될 수 있다.
    abort "알림 설정/전송 실패: #{error.class} (자격 증명과 서버 응답은 출력하지 않습니다.)"
  end
end
