#!/usr/bin/env ruby
# frozen_string_literal: true

require "yaml"

usage = <<~USAGE.freeze
  사용법:
    #{$PROGRAM_NAME} --references <release manifest>
    #{$PROGRAM_NAME} --inventory <release manifest> <runtime-images.env>
USAGE

mode, manifest, metadata = ARGV
valid_arguments = (mode == "--references" && ARGV.length == 2) ||
                  (mode == "--inventory" && ARGV.length == 3)
abort usage unless valid_arguments
abort "release manifest를 찾을 수 없습니다: #{manifest}" unless File.file?(manifest)
if mode == "--inventory"
  abort "runtime image metadata를 찾을 수 없습니다: #{metadata}" unless File.file?(metadata)
end

targets = {
  "MYSQL" => ["StatefulSet", "mysql", "mysql"],
  "REDIS" => ["Deployment", "redis", "redis"],
  "PROMETHEUS" => ["Deployment", "prometheus", "prometheus"],
  "ALERTMANAGER" => ["Deployment", "alertmanager", "alertmanager"],
  "GRAFANA" => ["Deployment", "grafana", "grafana"]
}.freeze

documents = YAML.load_stream(File.read(manifest)).compact

references = targets.map do |key, (kind, workload_name, container_name)|
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

  [key, image]
end

if mode == "--references"
  references.each { |key, image| puts "#{key}\t#{image}" }
  exit
end

metadata_values = {}
File.foreach(metadata).with_index(1) do |line, line_number|
  line = line.chomp
  next if line.empty? || line.match?(/\A[[:space:]]*#/)

  match = line.match(/\A([A-Z][A-Z0-9_]*)=([^\r\n]*)\z/)
  abort "runtime image metadata 형식이 올바르지 않습니다(#{line_number}행)." unless match

  key = match[1]
  abort "runtime image metadata에 중복 키가 있습니다: #{key}" if metadata_values.key?(key)

  metadata_values[key] = match[2]
end

expected_metadata_keys = targets.keys.flat_map { |key| ["#{key}_IMAGE", "#{key}_IMAGE_DIGEST"] }
unknown_keys = metadata_values.keys - expected_metadata_keys
missing_keys = expected_metadata_keys - metadata_values.keys
abort "알 수 없는 runtime image metadata 키입니다: #{unknown_keys.join(', ')}" if unknown_keys.any?
abort "runtime image metadata 키가 누락됐습니다: #{missing_keys.join(', ')}" if missing_keys.any?

references.each do |key, manifest_image|
  metadata_image = metadata_values.fetch("#{key}_IMAGE")
  digest = metadata_values.fetch("#{key}_IMAGE_DIGEST")
  abort "#{key} 이미지가 release manifest와 runtime metadata에서 다릅니다." unless metadata_image == manifest_image
  abort "#{key} runtime 이미지 digest가 올바르지 않습니다." unless digest.match?(/\Asha256:[a-f0-9]{64}\z/)

  puts "#{key}\t#{manifest_image}\t#{digest}"
end
