# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'fileutils'
require 'open3'
require 'net/smtp'
require_relative '../alert-delivery'

class AlertDeliveryTest < Minitest::Test
  def setup
    @dir = Dir.mktmpdir('happygallery-alert-test')
    @app = File.join(@dir, 'app.env')
    @alert = File.join(@dir, 'alertmanager.env')
    @output = File.join(@dir, 'output')
    Dir.mkdir(@output, 0o700)
    @values = {
      'EMAIL_VERIFICATION_PROVIDER' => 'smtp',
      'EMAIL_VERIFICATION_SMTP_HOST' => 'smtp.resend.com',
      'EMAIL_VERIFICATION_SMTP_PORT' => '587',
      'EMAIL_VERIFICATION_SMTP_USERNAME' => 'resend',
      'EMAIL_VERIFICATION_SMTP_PASSWORD' => 'test-secret+with/=characters',
      'EMAIL_VERIFICATION_FROM' => 'alerts@mail.happy-gallery.com',
      'EMAIL_VERIFICATION_STARTTLS_ENABLED' => 'true',
      'EMAIL_VERIFICATION_SSL_ENABLED' => 'false'
    }
    write_app
    private_write(@alert, "ALERT_EMAIL_TO=operator@example.org\n")
  end

  def teardown
    FileUtils.remove_entry(@dir)
  end

  def private_write(path, content)
    File.write(path, content)
    File.chmod(0o600, path)
  end

  def write_app
    private_write(@app, @values.map { |key, value| "#{key}=#{value}\n" }.join)
  end

  def render
    AlertDelivery.render(@app, @alert, @output)
    JSON.parse(File.read(File.join(@output, 'alertmanager.yml')))
  end

  def test_smtp_secret_preserves_credentials_and_routes_without_publishing_password
    config = render
    assert_equal %w[alertmanager.yml smtp-password], Dir.children(@output).sort
    assert_equal @values['EMAIL_VERIFICATION_SMTP_PASSWORD'], File.read(File.join(@output, 'smtp-password'))
    refute_includes JSON.generate(config), @values['EMAIL_VERIFICATION_SMTP_PASSWORD']
    Dir.children(@output).each { |name| assert_equal 0, File.stat(File.join(@output, name)).mode & 0o077 }
    assert_equal true, config.dig('global', 'smtp_require_tls')
    assert_equal 'smtp.resend.com', config.dig('global', 'smtp_tls_config', 'server_name')
    assert_equal %w[1h 30m 4h], config['route']['routes'].map { |route| route['repeat_interval'] }
    config['receivers'].each do |receiver|
      assert_nil receiver['webhook_configs']
      assert_equal [{ 'to' => 'operator@example.org', 'send_resolved' => true }], receiver['email_configs']
    end
  end

  def test_existing_webhook_works_without_smtp_credentials
    private_write(@alert, "https://alerts.invalid/hook?token=abc\n")
    config = AlertDelivery.render('/missing-app.env', @alert, @output)
    assert_equal %w[alertmanager.yml webhook-url], Dir.children(@output).sort
    config = JSON.parse(File.read(File.join(@output, 'alertmanager.yml')))
    assert_equal 'webhook-warning', config.dig('route', 'receiver')
    assert_nil config.dig('global', 'smtp_auth_password_file')
  end

  def test_rejects_invalid_email_settings_before_writing_any_secret
    [
      "ALERT_EMAIL_TO=\n", "ALERT_EMAIL_TO=one@example.org,two@example.org\n",
      "ALERT_EMAIL_TO=operator@example.org\nALERT_EMAIL_TO=second@example.org\n",
      "ALERT_EMAIL_TO=operator@example.org\nUNKNOWN=secret\n"
    ].each do |contents|
      private_write(@alert, contents)
      assert_raises(AlertDelivery::ConfigError) { render }
      assert_empty Dir.children(@output)
    end
  end

  def test_rejects_insecure_transport_and_wrong_provider
    {
      'EMAIL_VERIFICATION_STARTTLS_ENABLED' => 'false',
      'EMAIL_VERIFICATION_SSL_ENABLED' => 'true',
      'EMAIL_VERIFICATION_SMTP_PORT' => '465',
      'EMAIL_VERIFICATION_PROVIDER' => 'ncp',
      'EMAIL_VERIFICATION_SMTP_PASSWORD' => ''
    }.each do |key, value|
      original = @values[key]
      @values[key] = value
      write_app
      assert_raises(AlertDelivery::ConfigError) { render }
      assert_empty Dir.children(@output)
      @values[key] = original
    end
  end

  def test_rejects_public_permissions_and_does_not_echo_secret_in_errors
    File.chmod(0o644, @app)
    assert_raises(AlertDelivery::ConfigError) { render }
    private_write(@app, "SECRET=test-sensitive-value\r\n")
    output, status = Open3.capture2e('ruby', File.expand_path('../alert-delivery.rb', __dir__),
                                    'render', @app, @alert, @output)
    refute status.success?
    refute_includes output, 'test-sensitive-value'
    assert_empty Dir.children(@output)
  end

  def test_rejects_invalid_webhook_and_occupied_output_directory
    %W[https://example.com/hook https://one.invalid\nhttps://two.invalid https://user:secret@one.invalid/hook].each do |url|
      private_write(@alert, url)
      assert_raises(StandardError) { render }
      assert_empty Dir.children(@output)
    end
    private_write(@alert, "ALERT_EMAIL_TO=operator@example.org\n")
    private_write(File.join(@output, 'existing'), 'preserve')
    assert_raises(AlertDelivery::ConfigError) { render }
    assert_equal 'preserve', File.read(File.join(@output, 'existing'))
  end

  def test_standalone_secret_rejects_invalid_input_before_calling_kubectl
    private_write(@alert, "ALERT_EMAIL_TO=invalid\n")
    kube = File.join(@dir, 'kubectl')
    marker = File.join(@dir, 'called')
    private_write(kube, "#!/bin/sh\ntouch \"$HG_TEST_KUBE_MARKER\"\n")
    File.chmod(0o700, kube)
    _, status = Open3.capture2e({ 'KUBECTL_BIN' => kube, 'HG_TEST_KUBE_MARKER' => marker },
                              'bash', File.expand_path('../create-alertmanager-secret.sh', __dir__), @app, @alert)
    refute status.success?
    refute File.exist?(marker)
  end

  def test_backup_mail_requires_starttls_and_sends_utf8_body
    session = Minitest::Mock.new
    session.expect(:open_timeout=, 3, [3])
    session.expect(:read_timeout=, 10, [10])
    session.expect(:enable_starttls, nil) { |context| context.verify_mode == OpenSSL::SSL::VERIFY_PEER }
    session.expect(:start, nil) do |helo, username, password, auth, &block|
      assert_equal ['happy-gallery.com', 'resend', @values['EMAIL_VERIFICATION_SMTP_PASSWORD'], :plain],
                   [helo, username, password, auth]
      block.call(session)
      true
    end
    session.expect(:send_message, nil) do |message, from, to|
      assert_equal @values['EMAIL_VERIFICATION_FROM'], from
      assert_equal 'operator@example.org', to
      assert_includes message, ["백업 실패"].pack('m0')
      true
    end
    Net::SMTP.stub(:new, session) do
      AlertDelivery.send_email(@app, @alert, '백업 경보', '백업 실패')
    end
    session.verify
  end
end
