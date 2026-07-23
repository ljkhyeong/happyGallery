#!/usr/bin/env ruby
# frozen_string_literal: true

require "yaml"

abort "사용법: #{$PROGRAM_NAME} <release manifest>" unless ARGV.one?

manifest = ARGV.fetch(0)
abort "release manifest를 찾을 수 없습니다: #{manifest}" unless File.file?(manifest)

targets = {
  "MYSQL_IMAGE" => ["StatefulSet", "mysql", "mysql"],
  "REDIS_IMAGE" => ["Deployment", "redis", "redis"],
  "PROMETHEUS_IMAGE" => ["Deployment", "prometheus", "prometheus"],
  "ALERTMANAGER_IMAGE" => ["Deployment", "alertmanager", "alertmanager"]
}.freeze

documents = YAML.load_stream(File.read(manifest)).compact

targets.each do |key, (kind, workload_name, container_name)|
  workloads = documents.select do |document|
    document["kind"] == kind && document.dig("metadata", "name") == workload_name
  end
  abort "#{kind}/#{workload_name}가 release manifest에 정확히 하나 있어야 합니다." unless workloads.one?

  containers = workloads.first.dig("spec", "template", "spec", "containers")
  abort "#{kind}/#{workload_name}의 containers가 올바르지 않습니다." unless containers.is_a?(Array)

  matches = containers.select { |container| container["name"] == container_name }
  abort "#{kind}/#{workload_name}의 #{container_name} container가 정확히 하나 있어야 합니다." unless matches.one?

  image = matches.first["image"]
  safe_reference = image.is_a?(String) && image.match?(/\A[A-Za-z0-9][A-Za-z0-9._\/:@-]*\z/)
  digest_pinned = safe_reference && image.match?(/@sha256:[a-f0-9]{64}\z/)
  name_and_tag = safe_reference ? image.split("@", 2).first : ""
  tag_match = name_and_tag.split("/").last&.match(/:([A-Za-z0-9_][A-Za-z0-9_.-]{0,127})\z/)
  tag = tag_match && tag_match[1]
  valid_image = safe_reference &&
                (digest_pinned || (!image.include?("@") && !tag.nil?)) &&
                tag != "latest"
  abort "#{kind}/#{workload_name}의 image 참조가 고정된 tag 또는 sha256 digest가 아닙니다." unless valid_image

  puts "#{key}=#{image}"
end
