# frozen_string_literal: true

require 'json'

# 문서 키와 사용자 정의 필드 이름을 구분하고 요청/응답 방향으로 계약을 비교한다.
class OpenapiCompatibility
  DOCUMENTATION = %w[summary description externalDocs example examples title deprecated tags].freeze
  MAPS = %w[paths components schemas properties patternProperties $defs definitions responses content
            headers requestBodies securitySchemes callbacks links encoding].freeze
  CONSTRAINTS = %w[type format enum const default minimum maximum exclusiveMinimum exclusiveMaximum
                   multipleOf minLength maxLength pattern minItems maxItems uniqueItems minProperties
                   maxProperties additionalProperties unevaluatedProperties allOf anyOf oneOf not
                   nullable readOnly writeOnly discriminator security $ref contains minContains maxContains
                   propertyNames dependentRequired dependentSchemas if then else prefixItems items
                   additionalItems unevaluatedItems].freeze

  def initialize(old_spec, new_spec)
    @old_spec, @new_spec = old_spec, new_spec
    @visited = {}
    @errors = []
  end

  def differences
    compare(@old_spec, @new_spec, [], :response)
    @errors.uniq
  end

  private

  def error(path, reason)
    @errors << "#{path.join('/')} — #{reason}"
  end

  def resolve(spec, reference)
    return nil unless reference.is_a?(String) && reference.start_with?('#/')

    reference.delete_prefix('#/').split('/').reduce(spec) do |node, key|
      node.is_a?(Hash) ? node[key.gsub('~1', '/').gsub('~0', '~')] : nil
    end
  end

  def dereference(spec, value)
    seen = []
    while value.is_a?(Hash) && value.key?('$ref')
      reference = value['$ref']
      return nil if seen.include?(reference)
      seen << reference
      value = resolve(spec, reference)
    end
    value
  end

  def compare(old, fresh, path, direction, map: false)
    unless old.is_a?(Hash) && fresh.is_a?(Hash)
      error(path, '기존 값 또는 타입 변경') unless old == fresh
      return
    end

    # 같은 schema라도 요청과 응답에서 각각 검사한다. 순환 $ref는 방문 쌍으로 종료한다.
    if !map && old['$ref'] && old['$ref'] == fresh['$ref']
      reference = old['$ref']
      identity = [reference, direction]
      unless @visited[identity]
        @visited[identity] = true
        left, right = resolve(@old_spec, reference), resolve(@new_spec, reference)
        if left && right
          compare(left, right, path + [reference], direction)
        else
          error(path, '참조 계약을 확인할 수 없음')
        end
      end
    end

    (old.keys | fresh.keys).each do |key|
      child_path = path + [key]
      next if !map && (DOCUMENTATION.include?(key) || (path.empty? && key == 'info'))
      next if path == ['components'] && key == 'examples'

      if map
        if !fresh.key?(key)
          error(child_path, '기존 필드·경로 삭제')
        elsif old.key?(key)
          component_direction = if path == ['components']
                                  { 'schemas' => :component, 'parameters' => :request, 'requestBodies' => :request }.fetch(key, :response)
                                else
                                  direction
                                end
          compare(old[key], fresh[key], child_path, component_direction, map: path.last == 'components')
        end
      elsif key == 'required'
        compare_required(old[key], fresh[key], child_path, direction)
      elsif key == 'parameters' && path != ['components']
        compare_parameters(old[key] || [], fresh[key] || [], child_path)
      elsif key == 'requestBody'
        compare_body(old, fresh, child_path)
      elsif !fresh.key?(key)
        error(child_path, '기존 필드·조건 삭제')
      elsif !old.key?(key)
        error(child_path, '기존 계약에 제약 추가') if CONSTRAINTS.include?(key)
      elsif %w[allOf anyOf oneOf].include?(key) && old[key].is_a?(Array) && fresh[key].is_a?(Array)
        compare_alternatives(old[key], fresh[key], child_path, direction)
      elsif %w[enum type security].include?(key) && old[key].is_a?(Array) && fresh[key].is_a?(Array)
        # 배열의 순서에는 의존하지 않지만 항목 변경은 계약 변경이다.
        error(child_path, '기존 조건 변경') unless unordered(old[key]) == unordered(fresh[key])
      else
        next_direction = key == 'requestBodies' ? :request : direction
        compare(old[key], fresh[key], child_path, next_direction, map: MAPS.include?(key) || (path == ['components'] && key == 'parameters'))
      end
    end
  end

  def unordered(value)
    case value
    when Hash then value.keys.sort.to_h { |key| [key, unordered(value[key])] }
    when Array then value.map { |item| unordered(item) }.sort_by { |item| JSON.generate(item) }
    else value
    end
  end

  def compare_alternatives(old, fresh, path, direction)
    candidates = fresh.dup
    compatible = old.length == fresh.length && old.all? do |schema|
      index = candidates.index do |candidate|
        saved_errors, saved_visited = @errors, @visited
        begin
          @errors, @visited = [], saved_visited.dup
          compare(schema, candidate, path, direction)
          @errors.empty?
        ensure
          @errors, @visited = saved_errors, saved_visited
        end
      end
      candidates.delete_at(index) if index
      !index.nil?
    end
    error(path, '기존 schema 조합 변경') unless compatible
  end

  def compare_required(old, fresh, path, direction)
    if old.is_a?(Array) || fresh.is_a?(Array)
      left, right = old || [], fresh || []
      unless left.is_a?(Array) && right.is_a?(Array)
        error(path, 'required 형식 변경')
        return
      end
      if direction == :request
        error(path, "요청 필수 필드 추가: #{(right - left).join(', ')}") unless (right - left).empty?
      elsif direction == :response
        error(path, "응답 필수 보장 제거: #{(left - right).join(', ')}") unless (left - right).empty?
      end
    elsif (old || false) != (fresh || false)
      error(path, '필수 여부 변경')
    end
  end

  def compare_parameters(old, fresh, path)
    unless old.is_a?(Array) && fresh.is_a?(Array)
      error(path, 'parameters 형식 변경')
      return
    end
    index = lambda do |parameters, spec|
      parameters.to_h do |parameter|
        resolved = dereference(spec, parameter)
        [[resolved&.fetch('in', nil), resolved&.fetch('name', nil)], parameter]
      end
    end
    left, right = index.call(old, @old_spec), index.call(fresh, @new_spec)
    left.each do |identity, parameter|
      if right.key?(identity)
        compare(parameter, right[identity], path + identity, :request)
      else
        error(path + identity, '기존 요청 파라미터 삭제')
      end
    end
    (right.keys - left.keys).each do |identity|
      parameter = dereference(@new_spec, right[identity])
      error(path + identity, '필수 요청 파라미터 추가 또는 참조 불명') unless parameter && parameter['required'] != true
    end
  end

  def compare_body(old, fresh, path)
    if !fresh.key?('requestBody')
      error(path, '기존 요청 본문 삭제')
    elsif old.key?('requestBody')
      compare(old['requestBody'], fresh['requestBody'], path, :request)
    else
      body = dereference(@new_spec, fresh['requestBody'])
      error(path, '필수 요청 본문 추가 또는 참조 불명') unless body && body['required'] != true
    end
  end
end
