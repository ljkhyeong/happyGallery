# frozen_string_literal: true

require 'minitest/autorun'
require_relative '../openapi-compatibility'

class OpenapiCompatibilityTest < Minitest::Test
  def spec(schema, request: false)
    content = { 'content' => { 'application/json' => { 'schema' => schema } } }
    operation = request ? { 'requestBody' => content } : { 'responses' => { '200' => content } }
    { 'paths' => { '/sample' => { 'post' => operation } } }
  end

  def differences(old, fresh)
    OpenapiCompatibility.new(old, fresh).differences
  end

  def test_allows_documentation_and_required_order_changes
    old = { 'type' => 'object', 'required' => %w[id name],
            'description' => 'old', 'example' => { 'id' => 1 },
            'properties' => { 'id' => { 'type' => 'integer' }, 'name' => { 'type' => 'string' } } }
    fresh = Marshal.load(Marshal.dump(old))
    fresh.merge!('required' => %w[name id], 'description' => 'new', 'example' => { 'id' => 2 })
    [true, false].each { |request| assert_empty differences(spec(old, request: request), spec(fresh, request: request)) }
  end

  def test_does_not_treat_property_or_component_names_as_documentation
    %w[description summary example required parameters].each do |name|
      old = spec({ 'type' => 'object', 'properties' => { name => { 'type' => 'string' } } })
      fresh = spec({ 'type' => 'object', 'properties' => {} })
      assert differences(old, fresh).any? { |error| error.include?("properties/#{name}") }, name
    end
    assert differences({ 'components' => { 'schemas' => { 'description' => { 'type' => 'string' } } } },
                       { 'components' => { 'schemas' => {} } }).any?
  end

  def test_distinguishes_request_and_response_required_additions
    old = { 'type' => 'object', 'required' => ['id'], 'properties' => { 'id' => { 'type' => 'integer' } } }
    fresh = Marshal.load(Marshal.dump(old))
    fresh['properties']['name'] = { 'type' => 'string' }
    fresh['required'] << 'name'
    assert_empty differences(spec(old), spec(fresh))
    assert differences(spec(old, request: true), spec(fresh, request: true)).any? { |error| error.include?('요청 필수 필드 추가') }
    old.delete('required')
    assert differences(spec(old, request: true), spec(fresh, request: true)).any?
  end

  def test_rejects_type_changes_and_new_constraints
    old = { 'type' => 'string' }
    [{ 'type' => 'integer' }, { 'type' => 'string', 'minLength' => 5 }, { 'type' => 'string', 'pattern' => '^A' }].each do |fresh|
      assert differences(spec(old, request: true), spec(fresh, request: true)).any?, fresh.inspect
    end
  end

  def test_allows_optional_body_but_rejects_required_body
    old = { 'paths' => { '/sample' => { 'post' => { 'responses' => { '200' => {} } } } } }
    fresh = Marshal.load(Marshal.dump(old))
    fresh['paths']['/sample']['post']['requestBody'] = { 'required' => false, 'content' => {} }
    assert_empty differences(old, fresh)
    fresh['paths']['/sample']['post']['requestBody']['required'] = true
    assert differences(old, fresh).any?
  end

  def test_resolves_shared_recursive_schemas_in_both_directions
    old = spec({ '$ref' => '#/components/schemas/Node' }, request: true)
    old['paths']['/sample']['post']['responses'] = { '200' => { 'content' => {
      'application/json' => { 'schema' => { '$ref' => '#/components/schemas/Node' } }
    } } }
    old['components'] = { 'schemas' => { 'Node' => { 'type' => 'object', 'properties' => {
      'next' => { '$ref' => '#/components/schemas/Node' }, 'id' => { 'type' => 'integer' }
    } } } }
    fresh = Marshal.load(Marshal.dump(old))
    fresh['components']['schemas']['Node']['required'] = ['id']
    errors = differences(old, fresh)
    assert errors.any? { |error| error.include?('requestBody') && error.include?('요청 필수 필드 추가') }
    refute errors.any? { |error| error.include?('/responses/') }
  end

  def test_resolves_required_parameter_and_body_references
    old = { 'paths' => { '/sample' => { 'get' => { 'responses' => { '200' => {} } } } } }
    fresh = Marshal.load(Marshal.dump(old))
    fresh['components'] = {
      'parameters' => { 'Token' => { 'name' => 'token', 'in' => 'query', 'required' => true } },
      'requestBodies' => { 'Body' => { 'required' => true, 'content' => {} } }
    }
    operation = fresh['paths']['/sample']['get']
    operation['parameters'] = [{ '$ref' => '#/components/parameters/Token' }]
    operation['requestBody'] = { '$ref' => '#/components/requestBodies/Body' }
    errors = differences(old, fresh)
    assert errors.any? { |error| error.include?('필수 요청 파라미터') }
    assert errors.any? { |error| error.include?('필수 요청 본문') }
  end

  def test_allows_parameter_order_but_rejects_security_change
    parameters = [{ 'in' => 'query', 'name' => 'a' }, { 'in' => 'query', 'name' => 'b' }]
    old = { 'parameters' => parameters, 'security' => [{ 'bearer' => [] }] }
    fresh = { 'parameters' => parameters.reverse, 'security' => [{ 'bearer' => [] }] }
    assert_empty differences(old, fresh)
    fresh['security'] << {}
    assert differences(old, fresh).any?
  end

  def test_allows_documentation_changes_inside_reordered_composition
    old = spec({ 'allOf' => [{ 'type' => 'object', 'description' => 'old' }, { 'properties' => { 'id' => { 'type' => 'integer' } } }] })
    fresh = spec({ 'allOf' => [{ 'properties' => { 'id' => { 'type' => 'integer' } } }, { 'type' => 'object', 'description' => 'new' }] })
    assert_empty differences(old, fresh)
    fresh['paths']['/sample']['post']['responses']['200']['content']['application/json']['schema']['allOf'].pop
    assert differences(old, fresh).any?
  end

  def test_request_only_component_does_not_gain_response_rules
    old = spec({ '$ref' => '#/components/schemas/Input' }, request: true)
    old['components'] = { 'schemas' => { 'Input' => { 'required' => ['id'], 'properties' => { 'id' => { 'type' => 'integer' } } } } }
    fresh = Marshal.load(Marshal.dump(old))
    fresh['components']['schemas']['Input'].delete('required')
    assert_empty differences(old, fresh)
    # 동일 schema가 응답에도 쓰이면 필수 보장 제거를 감지한다.
    [old, fresh].each do |document|
      document['paths']['/sample']['get'] = { 'responses' => { '200' => { 'content' => {
        'application/json' => { 'schema' => { '$ref' => '#/components/schemas/Input' } }
      } } } }
    end
    assert differences(old, fresh).any? { |error| error.include?('응답 필수 보장 제거') }
  end
end
