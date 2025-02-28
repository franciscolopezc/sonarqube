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
import * as React from 'react';
import { getIssueChangelog } from '../../../api/issues';
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import { PopupPlacement } from '../../../components/ui/popups';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue, IssueChangelog } from '../../../types/types';
import DateTimeFormatter from '../../intl/DateTimeFormatter';
import Avatar from '../../ui/Avatar';
import IssueChangelogDiff from '../components/IssueChangelogDiff';

interface Props {
  issue: Pick<Issue, 'author' | 'creationDate' | 'key'>;
}

interface State {
  changelog: IssueChangelog[];
}

export default class ChangelogPopup extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { changelog: [] };

  componentDidMount() {
    this.mounted = true;
    this.loadChangelog();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadChangelog() {
    getIssueChangelog(this.props.issue.key).then(
      ({ changelog }) => {
        if (this.mounted) {
          this.setState({ changelog });
        }
      },
      () => {}
    );
  }

  render() {
    const { issue } = this.props;
    const { author } = issue;
    return (
      <DropdownOverlay placement={PopupPlacement.BottomRight}>
        <div className="menu is-container issue-changelog">
          <table className="spaced">
            <tbody>
              <tr>
                <td className="thin text-left text-top nowrap">
                  <DateTimeFormatter date={issue.creationDate} />
                </td>
                <td className="text-left text-top">
                  {author ? `${translate('created_by')} ${author}` : translate('created')}
                </td>
              </tr>

              {this.state.changelog.map((item, idx) => {
                const userName = item.userName || item.user || item.externalUser;

                return (
                  <tr key={idx}>
                    <td className="thin text-left text-top nowrap">
                      <DateTimeFormatter date={item.creationDate} />
                    </td>
                    <td className="text-left text-top">
                      <div>
                        {userName && (
                          <>
                            <Avatar
                              className="little-spacer-right"
                              hash={item.avatar}
                              name={userName}
                              size={16}
                            />
                            {item.isUserActive || item.externalUser
                              ? userName
                              : translateWithParameters('user.x_deleted', userName)}
                          </>
                        )}
                        {item.webhookSource &&
                          translateWithParameters(
                            'issue.changelog.webhook_source',
                            item.webhookSource
                          )}
                      </div>
                      {item.diffs.map((diff) => (
                        <IssueChangelogDiff diff={diff} key={diff.key} />
                      ))}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </DropdownOverlay>
    );
  }
}
