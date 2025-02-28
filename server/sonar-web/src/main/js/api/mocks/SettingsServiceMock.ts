/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { cloneDeep, isArray, isObject, isString } from 'lodash';
import { HousekeepingPolicy } from '../../apps/audit-logs/utils';
import { mockDefinition, mockSettingFieldDefinition } from '../../helpers/mocks/settings';
import { BranchParameters } from '../../types/branch-like';
import {
  ExtendedSettingDefinition,
  SettingDefinition,
  SettingsKey,
  SettingType,
  SettingValue,
} from '../../types/settings';
import {
  checkSecretKey,
  encryptValue,
  generateSecretKey,
  getAllValues,
  getDefinitions,
  getValue,
  getValues,
  resetSettingValue,
  setSettingValue,
} from '../settings';

const isEmptyField = (o: any) => isObject(o) && Object.values(o).some(isEmptyString);
const isEmptyString = (i: any) => isString(i) && i.trim() === '';

export const DEFAULT_DEFINITIONS_MOCK = [
  mockDefinition({
    category: 'general',
    key: 'sonar.announcement.message',
    subCategory: 'Announcement',
    name: 'Announcement message',
    description: 'Enter message',
    type: SettingType.TEXT,
  }),
  mockDefinition({
    category: 'general',
    key: 'sonar.ce.parallelProjectTasks',
    subCategory: 'Compute Engine',
    name: 'Run analysis in paralel',
    description: 'Enter message',
    type: SettingType.TEXT,
  }),
  mockDefinition({
    category: 'javascript',
    key: 'sonar.javascript.globals',
    subCategory: 'General',
    name: 'Global Variables',
    description: 'List of Global variables',
    multiValues: true,
    defaultValue: 'angular,google,d3',
  }),
  mockDefinition({
    category: 'javascript',
    key: 'sonar.javascript.file.suffixes',
    subCategory: 'General',
    name: 'JavaScript File Suffixes',
    description: 'List of suffixes for files to analyze',
    multiValues: true,
    defaultValue: '.js,.jsx,.cjs,.vue,.mjs',
  }),
  mockDefinition({
    category: 'External Analyzers',
    key: 'sonar.androidLint.reportPaths',
    subCategory: 'Android',
    name: 'Android Lint Report Files',
    description: 'Paths to xml files',
    multiValues: true,
  }),
  mockDefinition({
    category: 'COBOL',
    key: 'sonar.cobol.compilationConstants',
    subCategory: 'Preprocessor',
    name: 'Compilation Constants',
    description: 'Lets do it',
    type: SettingType.PROPERTY_SET,
    fields: [
      mockSettingFieldDefinition(),
      mockSettingFieldDefinition({ key: 'value', name: 'Value' }),
    ],
  }),
];

export default class SettingsServiceMock {
  #defaultValues: SettingValue[] = [
    {
      key: SettingsKey.AuditHouseKeeping,
      value: HousekeepingPolicy.Weekly,
    },
    {
      key: 'sonar.javascript.globals',
      values: ['angular', 'google', 'd3'],
    },
  ];

  #settingValues: SettingValue[] = cloneDeep(this.#defaultValues);

  #definitions: ExtendedSettingDefinition[] = cloneDeep(DEFAULT_DEFINITIONS_MOCK);

  #secretKeyAvailable: boolean = false;

  constructor() {
    jest.mocked(getDefinitions).mockImplementation(this.handleGetDefinitions);
    jest.mocked(getValue).mockImplementation(this.handleGetValue);
    jest.mocked(getValues).mockImplementation(this.handleGetValues);
    jest.mocked(getAllValues).mockImplementation(this.handleGetAllValues);
    jest.mocked(setSettingValue).mockImplementation(this.handleSetSettingValue);
    jest.mocked(resetSettingValue).mockImplementation(this.handleResetSettingValue);
    jest.mocked(checkSecretKey).mockImplementation(this.handleCheckSecretKey);
    jest.mocked(generateSecretKey).mockImplementation(this.handleGenerateSecretKey);
    jest.mocked(encryptValue).mockImplementation(this.handleEcnryptValue);
  }

  handleGetValue = (data: { key: string; component?: string } & BranchParameters) => {
    const setting = this.#settingValues.find((s) => s.key === data.key) as SettingValue;
    return this.reply(setting);
  };

  handleGetValues = (data: { keys: string[]; component?: string } & BranchParameters) => {
    const settings = this.#settingValues.filter((s) => data.keys.includes(s.key));
    return this.reply(settings);
  };

  handleGetAllValues = () => {
    return this.reply(this.#settingValues);
  };

  handleGetDefinitions = () => {
    return this.reply(this.#definitions);
  };

  handleSetSettingValue = (definition: SettingDefinition, value: any): Promise<void> => {
    if (
      isEmptyString(value) ||
      (isArray(value) && value.some(isEmptyString)) ||
      isEmptyField(value)
    ) {
      throw new ResponseError('validation error', {
        errors: [{ msg: 'A non empty value must be provided' }],
      });
    }

    this.set(definition.key, value);

    return this.reply(undefined);
  };

  handleResetSettingValue = (data: { keys: string; component?: string } & BranchParameters) => {
    const setting = this.#settingValues.find((s) => s.key === data.keys) as SettingValue;
    const definition = this.#definitions.find(
      (d) => d.key === data.keys
    ) as ExtendedSettingDefinition;
    if (definition.type === SettingType.PROPERTY_SET) {
      setting.fieldValues = [];
    } else if (definition.multiValues === true) {
      setting.values = definition.defaultValue?.split(',') ?? [];
    } else {
      setting.value = definition.defaultValue ?? '';
    }

    return this.reply(undefined);
  };

  emptySettings = () => {
    this.#settingValues = [];
    return this;
  };

  set = (key: string | SettingsKey, value: any) => {
    const setting = this.#settingValues.find((s) => s.key === key);
    if (setting) {
      setting.value = value;
      setting.values = value;
      setting.fieldValues = value;
    } else {
      this.#settingValues.push({ key, value, values: value, fieldValues: value });
    }
    return this;
  };

  setDefinition = (definition: ExtendedSettingDefinition) => {
    this.#definitions.push(definition);
  };

  handleCheckSecretKey = () => {
    return this.reply({ secretKeyAvailable: this.#secretKeyAvailable });
  };

  handleGenerateSecretKey = () => {
    return this.reply({ secretKey: 'secretKey' });
  };

  handleEcnryptValue = () => {
    return this.reply({ encryptedValue: 'encryptedValue' });
  };

  setSecretKeyAvailable = (val = false) => {
    this.#secretKeyAvailable = val;
  };

  reset = () => {
    this.#settingValues = cloneDeep(this.#defaultValues);
    this.#definitions = cloneDeep(DEFAULT_DEFINITIONS_MOCK);
    this.#secretKeyAvailable = false;
    return this;
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}

class ResponseError extends Error {
  #response: any;
  constructor(name: string, response: any) {
    super(name);
    this.#response = response;
  }

  json = () => Promise.resolve(this.#response);
}
