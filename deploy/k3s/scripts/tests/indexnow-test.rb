# frozen_string_literal: true

require 'minitest/autorun'
require 'open3'
require_relative '../notify-indexnow'

class IndexNowTest < Minitest::Test
  KEY = 'local-test-indexnow-key'

  def setup
    @directory = Dir.mktmpdir('happygallery-indexnow-test-')
    @state = File.join(@directory, 'pages.json')
    @catalogs = {
      'products' => [{ 'id' => 1, 'name' => '도자기 컵', 'price' => 30_000 }],
      'classes' => [{ 'id' => 2, 'name' => '도자기 수업' }],
      'events' => [{ 'id' => 3, 'title' => '가을 행사' }],
      'notices' => [{ 'id' => 4, 'title' => '추석 휴무', 'viewCount' => 0, 'version' => 0 }]
    }
    @responses = {}
    @requests = []
    @post_statuses = []
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def request(uri, body = nil)
    @requests << [uri, body]
    return [@post_statuses.shift || 200, ''] if body
    return @responses[uri.path] if @responses.key?(uri.path)
    return [200, KEY] if uri.path == "/#{KEY}.txt"
    [200, JSON.generate(@catalogs.fetch(uri.path.delete_prefix('/api/v1/')))]
  end

  def sync
    capture_io { IndexNow.stub(:request, method(:request)) { IndexNow.sync(@directory, key: KEY) } }
  end

  def submitted_paths
    @requests.map { |_uri, body| body && body['urlList'] }.compact.flatten.map { |url| URI(url).path }
  end

  def baseline
    sync
    @requests.clear
  end

  def test_first_run_saves_baseline_without_submitting_existing_pages
    output, = sync
    assert_match(/비교 기준 저장/, output)
    assert_empty submitted_paths
    assert_equal 8, IndexNow.read_state(@state).size
    assert_equal 0, File.stat(@state).mode & 0o077
    assert @requests.all? { |uri, body| uri.host == 'happy-gallery.com' && body.nil? }
  end

  def test_added_changed_and_deleted_pages_notify_details_lists_and_home
    baseline
    @catalogs['products'].first['price'] = 25_000
    @catalogs['classes'] << { 'id' => 5, 'name' => '물레 수업' }
    @catalogs['events'].clear
    @catalogs['notices'].first['version'] = 1
    sync
    assert_equal %w[/ /classes /classes/5 /events /events/3 /notices/4 /products /products/1], submitted_paths.sort
    endpoint, payload = @requests.find { |_uri, body| body }
    assert_equal 'https://api.indexnow.org/indexnow', endpoint.to_s
    assert_equal 'happy-gallery.com', payload['host']
    assert_equal KEY, payload['key']
    assert_equal "https://happy-gallery.com/#{KEY}.txt", payload['keyLocation']
    refute @requests.any? { |uri, _body| uri.path == '/api/v1/notices/4' }
    @requests.clear
    sync
    assert_empty submitted_paths
  end

  def test_order_view_count_and_json_key_order_do_not_trigger_submission
    @catalogs['products'] << { 'id' => 9, 'name' => '접시' }
    baseline
    original = File.read(@state)
    @catalogs['notices'].first['viewCount'] = 999
    @catalogs['products'].reverse!
    @catalogs['products'].map! { |row| row.to_a.reverse.to_h }
    output, = sync
    assert_match(/변경 없음/, output)
    assert_empty submitted_paths
    assert_equal original, File.read(@state)
    assert_equal 4, @requests.size
  end

  def test_api_error_or_invalid_list_never_submits_deletions_or_changes_state
    baseline
    original = File.read(@state)
    responses = [
      [503, 'maintenance'], [302, 'redirect'], [200, '<html>error</html>'],
      [200, '{"items":[]}'], [200, '[{"id":"secret"}]'], [200, '[{"id":1},{"id":1}]']
    ]
    responses.each do |response|
      @responses['/api/v1/events'] = response
      @catalogs['products'].clear
      assert_raises(IndexNow::Error) { sync }
      assert_empty submitted_paths
      assert_equal original, File.read(@state)
    end
  end

  def test_missing_notice_version_preserves_state
    baseline
    original = File.read(@state)
    @catalogs['notices'].first.delete('version')
    assert_raises(IndexNow::Error) { sync }
    assert_equal original, File.read(@state)
    assert_empty submitted_paths
  end

  def test_wrong_or_unavailable_key_file_prevents_baseline_and_submission
    [[200, 'wrong-key'], [404, ''], [301, 'redirect']].each do |response|
      @responses["/#{KEY}.txt"] = response
      assert_raises(IndexNow::Error) { sync }
      refute File.exist?(@state)
      assert_empty submitted_paths
    end
  end

  def test_rejected_submission_is_retried_next_run_and_202_means_accepted
    baseline
    original = File.read(@state)
    @catalogs['products'].clear
    [403, 422, 429, 500, 302].each do |status|
      @post_statuses << status
      assert_raises(IndexNow::Error) { sync }
      assert_equal original, File.read(@state)
    end
    @requests.clear
    @post_statuses << 202
    output, = sync
    assert_match(/HTTP 202/, output)
    assert_equal %w[/ /products /products/1], submitted_paths.sort
    refute IndexNow.read_state(@state).key?('/products/1')
  end

  def test_successful_batch_is_checkpointed_before_next_batch_failure
    baseline
    old = IndexNow.read_state(@state)
    # 공식 최대 묶음 크기를 넘겨도 접수한 묶음은 재전송하지 않는다.
    current = (1..10_001).to_h { |id| ["/products/#{id}", 'a' * 64] }
    @post_statuses = [200, 429]
    IndexNow.stub(:snapshot, current) { assert_raises(IndexNow::Error) { sync } }
    assert_equal [10_000, 8], @requests.map { |_uri, body| body && body['urlList'].size }.compact
    accepted = submitted_paths.take(10_000)
    @requests.clear
    IndexNow.stub(:snapshot, current) { sync }
    assert_empty accepted & submitted_paths
    assert_equal current, IndexNow.read_state(@state)
    assert_equal 8, old.size
  end

  def test_corrupt_state_and_concurrent_run_do_not_send_requests
    File.write(@state, '{')
    assert_raises(IndexNow::Error) { sync }
    assert_empty @requests
    File.delete(@state)
    File.open(File.join(@directory, 'sync.lock'), 'w') do |lock|
      lock.flock(File::LOCK_EX)
      assert_raises(IndexNow::Error) { sync }
      assert_empty @requests
    end
  end

  def test_network_timeout_during_submission_preserves_pending_changes
    baseline
    original = File.read(@state)
    @catalogs['products'].first['price'] = 20_000
    transport = lambda do |uri, body = nil|
      raise Net::ReadTimeout if body
      request(uri, body)
    end
    IndexNow.stub(:request, transport) do
      assert_raises(Net::ReadTimeout) { IndexNow.sync(@directory, key: KEY) }
    end
    assert_equal original, File.read(@state)
    sync
    assert_equal %w[/ /products /products/1], submitted_paths.sort
  end

  def test_disabled_cli_does_not_create_state_or_need_a_key
    output, error, status = Open3.capture3(
      { 'INDEXNOW_ENABLED' => 'false', 'INDEXNOW_KEY' => '' },
      RbConfig.ruby, File.expand_path('../notify-indexnow.rb', __dir__), File.join(@directory, 'disabled')
    )
    assert status.success?, error
    assert_match(/비활성/, output)
    refute File.exist?(File.join(@directory, 'disabled'))
  end
end
