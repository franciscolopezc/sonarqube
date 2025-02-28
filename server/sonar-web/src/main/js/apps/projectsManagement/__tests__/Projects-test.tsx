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
import { shallow } from 'enzyme';
import * as React from 'react';
import { ComponentQualifier, Visibility } from '../../../types/component';
import Projects from '../Projects';

const projects = [
  { key: 'a', name: 'A', qualifier: ComponentQualifier.Project, visibility: Visibility.Public },
  { key: 'b', name: 'B', qualifier: ComponentQualifier.Project, visibility: Visibility.Public },
];
const selection = ['a'];

it('renders list of projects', () => {
  expect(shallowRender({ projects, selection })).toMatchSnapshot();
});

it('selects and deselects project', () => {
  const onProjectDeselected = jest.fn();
  const onProjectSelected = jest.fn();
  const wrapper = shallowRender({ onProjectDeselected, onProjectSelected, projects });

  wrapper.find('ProjectRow').first().prop<Function>('onProjectCheck')(projects[0], true);
  expect(onProjectSelected).toHaveBeenCalledWith('a');

  wrapper.find('ProjectRow').first().prop<Function>('onProjectCheck')(projects[0], false);
  expect(onProjectDeselected).toHaveBeenCalledWith('a');
});

function shallowRender(props?: any) {
  return shallow(
    <Projects
      currentUser={{ login: 'foo' }}
      onProjectDeselected={jest.fn()}
      onProjectSelected={jest.fn()}
      selection={[]}
      {...props}
    />
  );
}
