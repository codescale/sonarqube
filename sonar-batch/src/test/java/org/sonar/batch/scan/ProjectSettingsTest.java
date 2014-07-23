/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.BootstrapProperties;
import org.sonar.batch.bootstrap.GlobalSettings;
import org.sonar.batch.protocol.input.GlobalReferentials;
import org.sonar.batch.settings.SettingsReferential;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectSettingsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  SettingsReferential settingsRef = mock(SettingsReferential.class);
  ProjectDefinition project = ProjectDefinition.create().setKey("struts");
  Configuration deprecatedConf = new BaseConfiguration();
  GlobalSettings bootstrapProps;

  private AnalysisMode mode;

  @Before
  public void prepare() {
    mode = mock(AnalysisMode.class);
    bootstrapProps = new GlobalSettings(new BootstrapProperties(Collections.<String, String>emptyMap()), new PropertyDefinitions(), new GlobalReferentials(), deprecatedConf, mode);
  }

  @Test
  public void should_load_project_props() {
    project.setProperty("project.prop", "project");

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, new PropertyDefinitions(), settingsRef, deprecatedConf, mode);

    assertThat(batchSettings.getString("project.prop")).isEqualTo("project");
  }

  @Test
  public void should_load_project_root_settings() {
    when(settingsRef.projectSettings("struts")).thenReturn(ImmutableMap.of("sonar.cpd.cross", "true", "sonar.java.coveragePlugin", "jacoco"));

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, new PropertyDefinitions(), settingsRef, deprecatedConf, mode);

    assertThat(batchSettings.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");
  }

  @Test
  public void should_load_project_root_settings_on_branch() {
    project.setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "mybranch");

    when(settingsRef.projectSettings("struts:mybranch")).thenReturn(ImmutableMap.of("sonar.cpd.cross", "true", "sonar.java.coveragePlugin", "jacoco"));

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, new PropertyDefinitions(), settingsRef, deprecatedConf, mode);

    assertThat(batchSettings.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");
  }

  @Test
  public void should_not_fail_when_accessing_secured_properties() {
    when(settingsRef.projectSettings("struts")).thenReturn(ImmutableMap.of("sonar.foo.secured", "bar", "sonar.foo.license.secured", "bar2"));

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, new PropertyDefinitions(), settingsRef, deprecatedConf, mode);

    assertThat(batchSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");
    assertThat(batchSettings.getString("sonar.foo.secured")).isEqualTo("bar");
  }

  @Test
  public void should_fail_when_accessing_secured_properties_in_dryrun() {
    when(settingsRef.projectSettings("struts")).thenReturn(ImmutableMap.of("sonar.foo.secured", "bar", "sonar.foo.license.secured", "bar2"));

    when(mode.isPreview()).thenReturn(true);

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, new PropertyDefinitions(), settingsRef, deprecatedConf, mode);

    assertThat(batchSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");
    thrown.expect(MessageException.class);
    thrown
      .expectMessage("Access to the secured property 'sonar.foo.secured' is not possible in preview mode. The SonarQube plugin which requires this property must be deactivated in preview mode.");
    batchSettings.getString("sonar.foo.secured");
  }

  @Test
  public void should_forward_to_deprecated_commons_configuration() {
    when(settingsRef.projectSettings("struts")).thenReturn(ImmutableMap.of("sonar.cpd.cross", "true", "sonar.java.coveragePlugin", "jacoco"));

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, new PropertyDefinitions(), settingsRef, deprecatedConf, mode);

    assertThat(deprecatedConf.getString("sonar.cpd.cross")).isEqualTo("true");
    assertThat(deprecatedConf.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");

    batchSettings.removeProperty("sonar.cpd.cross");
    assertThat(deprecatedConf.getString("sonar.cpd.cross")).isNull();
    assertThat(deprecatedConf.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");

    batchSettings.clear();
    assertThat(deprecatedConf.getString("sonar.cpd.cross")).isNull();
    assertThat(deprecatedConf.getString("sonar.java.coveragePlugin")).isNull();
  }

}
