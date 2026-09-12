# frozen_string_literal: true

require 'minitest/autorun'
require 'fileutils'
require 'open3'
require 'tmpdir'

class ContainerdImageTest < Minitest::Test
  COMMON = File.expand_path('../common.sh', __dir__)
  DIGEST = "sha256:#{'a' * 64}"

  def setup
    @directory = Dir.mktmpdir('happygallery-containerd-test-')
    @listing = File.join(@directory, 'images.txt')
    @k3s = File.join(@directory, 'k3s')
    File.write(@k3s, <<~'SH')
      #!/bin/sh
      [ "$*" = 'ctr images list' ] || exit 64
      if [ "$IMAGE_TEST_EXIT" = 0 ]; then
        exec cat "$IMAGE_TEST_LIST"
      fi
      cat "$IMAGE_TEST_LIST"
      exit "$IMAGE_TEST_EXIT"
    SH
    File.chmod(0o755, @k3s)
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def row(image, digest = DIGEST)
    "#{image} application/vnd.oci.image.manifest.v1+json #{digest} 1MiB linux/amd64 -\n"
  end

  def lookup(image, rows, producer_exit: 0)
    File.write(@listing, "REF TYPE DIGEST SIZE PLATFORMS LABELS\n#{rows}")
    Open3.capture3({ 'K3S_BIN' => @k3s, 'IMAGE_TEST_LIST' => @listing,
                    'IMAGE_TEST_EXIT' => producer_exit.to_s }, 'bash', '-c', <<~'SH', COMMON, COMMON, image)
      set -o pipefail
      . "$1"
      digest=$(containerd_image_digest "$2")
      printf '%s\n' "$digest"
    SH
  end

  def test_large_image_listing_does_not_break_the_producer_pipe
    rows = row('localhost/happygallery-app:release') + row('example.invalid/other:1') * 20_000
    output, error, status = lookup('localhost/happygallery-app:release', rows)
    assert status.success?, "exit=#{status.exitstatus}: #{error}"
    assert_equal "#{DIGEST}\n", output
  end

  def test_normalized_docker_hub_reference_matches
    output, error, status = lookup('mysql:8.4', row('docker.io/library/mysql:8.4'))
    assert status.success?, error
    assert_equal "#{DIGEST}\n", output
  end

  def test_multiple_matching_aliases_return_only_the_first_digest
    rows = row('docker.io/library/mysql:8.4') + row('mysql:8.4')
    output, error, status = lookup('mysql:8.4', rows)
    assert status.success?, error
    assert_equal "#{DIGEST}\n", output
  end

  def test_missing_image_is_rejected
    output, _error, status = lookup('mysql:8.4', row('mysql:8.40'))
    assert_equal 1, status.exitstatus
    assert_empty output
  end

  def test_containerd_failure_is_not_hidden_by_a_matching_row
    output, _error, status = lookup('mysql:8.4', row('mysql:8.4'), producer_exit: 23)
    assert_equal 23, status.exitstatus
    assert_empty output
  end
end
