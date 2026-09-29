require 'minitest/autorun'
require_relative '../security-update'

class SecurityUpdateTest < Minitest::Test
  def report(name, installed, fixed)
    { 'Results' => [{ 'Vulnerabilities' => [{ 'PkgName' => name, 'InstalledVersion' => installed,
      'FixedVersion' => fixed, 'Severity' => 'HIGH', 'VulnerabilityID' => 'CVE-test' }] }] }
  end

  def test_updates_both_jackson_boms_using_smallest_same_line_fix
    source = "ext['jackson-2-bom.version'] = '2.21.4'\next['jackson-bom.version'] = '3.1.4'\n"
    updated, changes, blocked = SecurityUpdate.plan(source, [
      report('com.fasterxml.jackson.core:jackson-databind', '2.21.4', '2.18.10, 2.21.6, 2.22.2'),
      report('tools.jackson.core:jackson-databind', '3.1.4', '3.2.2, 3.1.6')])
    assert_equal 2, changes.size
    assert_includes updated, "'2.21.6'"
    assert_includes updated, "'3.1.6'"
    assert_empty blocked
  end

  def test_never_guesses_unknown_missing_or_major_fix
    source = "ext['jackson-bom.version'] = '3.1.4'\n"
    [report('unknown:library', '1.0.0', '1.0.1'),
     report('tools.jackson.core:jackson-databind', '3.1.4', ''),
     report('tools.jackson.core:jackson-databind', '3.1.4', '4.0.0'),
     report('tools.jackson.core:jackson-databind', '3.1.2', '3.1.6')].each do |input|
      updated, changes, blocked = SecurityUpdate.plan(source, [input])
      assert_equal source, updated
      assert_empty changes
      refute_empty blocked
    end
  end

  def test_uses_highest_required_patch_across_findings
    source = "ext['jackson-bom.version'] = '3.1.4'"
    _, changes, = SecurityUpdate.plan(source, [report('tools.jackson.core:jackson-databind', '3.1.4', '3.1.6'),
      report('tools.jackson.core:jackson-core', '3.1.4', '3.1.7')])
    assert_equal '3.1.7', changes['jackson-bom.version']
  end
end
