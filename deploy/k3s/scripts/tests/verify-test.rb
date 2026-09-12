# frozen_string_literal: true

require 'minitest/autorun'
require 'fileutils'
require 'json'
require 'open3'
require 'tmpdir'

class DeploymentVerifyTest < Minitest::Test
  VERIFY = File.expand_path('../verify.sh', __dir__)
  HOST = 'https://happy-gallery.com'
  PRODUCTS = "#{HOST}/api/v1/products"
  DENIED = "#{HOST}/api/v1/definitely-not-a-route"

  def setup
    @directory = Dir.mktmpdir('happygallery-verify-test-')
    @responses = {
      'http://127.0.0.1:18081/actuator/health/readiness' => [200, 'application/json', '{"status":"UP"}'],
      'http://127.0.0.1:19090/api/v1/targets?state=active' =>
        [200, 'application/json', '{"targets":[{"url":"app-management:8081","health":"up"}]}'],
      'http://127.0.0.1:19090/api/v1/alertmanagers' =>
        [200, 'application/json', '{"activeAlertmanagers":[{"url":"alertmanager:9093"}]}'],
      'http://happy-gallery.com/' => [308, 'text/plain', ''],
      "#{HOST}/" => [200, 'text/html',
                      '<link rel="canonical" href="https://happy-gallery.com/">' \
                      '<h1>해피갤러리</h1><script nonce="verify-nonce"></script>'],
      "#{HOST}/robots.txt" => [200, 'text/plain', "Sitemap: #{HOST}/sitemap.xml\n"],
      "#{HOST}/sitemap.xml" => [200, 'application/xml',
                                 '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">' \
                                 '<url><loc>https://happy-gallery.com/</loc></url></urlset>'],
      "#{HOST}/__happygallery_verify_not_found__" => [404, 'text/html', '<h1>Not Found</h1>'],
      PRODUCTS => [200, 'application/json', '[]'],
      DENIED => [401, 'application/json', '{"code":"UNAUTHORIZED"}']
    }
    write_executable('kubectl', <<~'RUBY')
      command = ARGV.join(' ')
      if ARGV.include?('port-forward')
        exit 0
      elsif command.include?('.status.phase')
        puts 'Bound'
      elsif command.include?('.status.readyReplicas')
        puts '1'
      elsif command.include?('.spec.type')
        puts 'ClusterIP'
      elsif ARGV.include?('get') && (ARGV.include?('node') || ARGV.include?('pods'))
        puts 'ready'
      else
        abort "예상하지 않은 kubectl 호출: #{command}"
      end
    RUBY
    write_executable('curl', <<~'RUBY')
      require 'json'
      responses = JSON.parse(File.read(ENV.fetch('VERIFY_TEST_RESPONSES')))
      url = ARGV.find { |arg| arg.start_with?('http://', 'https://') }
      failure_mode = ENV.fetch('VERIFY_TEST_CONNECTION_FAILURE')
      if failure_mode != 'none' && url.end_with?('/actuator/health/readiness')
        marker = "#{ENV.fetch('VERIFY_TEST_RESPONSES')}.connected"
        if failure_mode == 'always' || !File.exist?(marker)
          File.write(marker, '')
          warn "curl: (7) Couldn't connect to server"
          exit 7
        end
      end
      status, type, body = responses.fetch(url)
      header_index = ARGV.index('-D')
      if header_index
        File.write(ARGV.fetch(header_index + 1),
                   "HTTP/1.1 #{status}\r\nContent-Type: #{type}\r\n" \
                   "Content-Security-Policy-Report-Only: script-src 'nonce-verify-nonce'\r\n\r\n")
      end
      output_index = ARGV.index('-o')
      output_index ? File.write(ARGV.fetch(output_index + 1), body) : print(body)
      print status if ARGV.include?('-w')
    RUBY
    write_executable('sleep', 'exit 0')
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def write_executable(name, body)
    path = File.join(@directory, name)
    File.write(path, "#!/usr/bin/env ruby\n#{body}")
    File.chmod(0o755, path)
  end

  def verify(connection_failure: 'none')
    responses = File.join(@directory, 'responses.json')
    File.write(responses, JSON.generate(@responses))
    Open3.capture3({ 'PATH' => "#{@directory}:#{ENV.fetch('PATH')}",
                    'KUBECTL_BIN' => File.join(@directory, 'kubectl'),
                    'LOCAL_MANAGEMENT_PORT' => '18081', 'LOCAL_PROMETHEUS_PORT' => '19090',
                    'SKIP_PUBLIC_CHECK' => 'false', 'VERIFY_TEST_RESPONSES' => responses,
                    'VERIFY_TEST_CONNECTION_FAILURE' => connection_failure }, 'sh', VERIFY, 'happy-gallery.com')
  end

  def assert_rejected(url, response, message)
    @responses[url] = response
    output, error, status = verify
    refute status.success?, output
    assert_includes error, message
  end

  def test_public_api_and_deny_by_default_json_responses_pass
    output, error, status = verify
    assert status.success?, error
    assert_includes output, 'API 경계 검증 완료'
  end

  def test_public_api_cannot_be_blocked_by_authentication
    assert_rejected(PRODUCTS, [401, 'application/json', '{"code":"UNAUTHORIZED"}'], '공개 상품 API가 200')
  end

  def test_public_api_requires_a_json_array
    assert_rejected(PRODUCTS, [200, 'application/json', '{}'], 'JSON 배열이 아닙니다')
  end

  def test_unlisted_api_cannot_become_public_or_return_an_unexpected_status
    [200, 404, 500].each do |status|
      assert_rejected(DENIED, [status, 'application/json', '{"code":"UNAUTHORIZED"}'], '401을 반환하지')
    end
  end

  def test_html_error_page_does_not_pass_as_an_api_error
    assert_rejected(DENIED, [401, 'text/html', '<!doctype html><h1>Unauthorized</h1>'], 'JSON이 아닌 응답')
  end

  def test_invalid_json_is_rejected_even_with_json_content_type
    assert_rejected(DENIED, [401, 'application/json', '<html>Unauthorized</html>'], 'UNAUTHORIZED JSON 오류')
  end

  def test_error_code_must_match_the_authentication_policy
    assert_rejected(DENIED, [401, 'application/json', '{"code":"NOT_FOUND"}'], 'UNAUTHORIZED JSON 오류')
  end

  def test_initial_port_forward_connection_error_is_quiet_after_retry_succeeds
    output, error, status = verify(connection_failure: 'once')
    assert status.success?, error
    assert_includes output, 'API 경계 검증 완료'
    refute_includes error, 'curl: (7)'
  end

  def test_persistent_port_forward_connection_error_is_reported
    output, error, status = verify(connection_failure: 'always')
    refute status.success?, output
    assert_includes error, 'curl: (7)'
    assert_includes error, '내부 Actuator readiness 확인에 실패'
  end
end
