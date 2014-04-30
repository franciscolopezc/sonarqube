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
package org.sonar.server.rule2;

import org.sonar.server.rule2.RuleNormalizer.RuleField;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.server.search.Hit;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Rule based on an Elasticsearch document
 */
class RuleDoc implements Rule {

  private final Map<String, Object> fields;

  RuleDoc(Map<String, Object> fields) {
    this.fields = fields;
  }

  RuleDoc(Hit hit) {
    this.fields = hit.getFields();
  }

  @Override
  public RuleKey key() {
    return RuleKey.of((String) fields.get(RuleField.REPOSITORY.key()),
      (String) fields.get(RuleField.KEY.key()));
  }

  @Override
  public String language() {
    return (String) fields.get(RuleField.LANGUAGE.key());
  }

  @Override
  public String name() {
    return (String) fields.get(RuleField.NAME.key());
  }

  @Override
  public String description() {
    return (String) fields.get(RuleField.DESCRIPTION.key());
  }

  @Override
  public String severity() {
    return (String) fields.get(RuleField.SEVERITY.key());
  }

  @Override
  public RuleStatus status() {
    return RuleStatus.valueOf((String) fields.get(RuleField.STATUS.key()));
  }

  @Override
  public boolean template() {
    return (Boolean) fields.get(RuleField.TEMPLATE.key());
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> tags() {
    return (List<String>) fields.get(RuleField.TAGS.key());
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> systemTags() {
    return (List<String>) fields.get(RuleField.SYSTEM_TAGS.key());
  }

  @Override
  public List<RuleParam> params() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public String debtCharacteristicKey() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public String debtSubCharacteristicKey() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public DebtRemediationFunction debtRemediationFunction() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public Date createdAt() {
    return (Date) fields.get(RuleField.CREATED_AT.key());
  }

  @Override
  public Date updatedAt() {
    return (Date) fields.get(RuleField.UDPATED_AT.key());
  }
}
