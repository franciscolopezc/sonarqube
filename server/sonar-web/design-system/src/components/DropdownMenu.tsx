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
import { css } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import React from 'react';
import tw from 'twin.macro';
import { INPUT_SIZES } from '../helpers/constants';
import { translate } from '../helpers/l10n';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { InputSizeKeys, ThemedProps } from '../types/theme';
import Checkbox from './Checkbox';
import { ClipboardBase } from './clipboard';
import { BaseLink, LinkProps } from './Link';
import NavLink from './NavLink';
import RadioButton from './RadioButton';
import Tooltip from './Tooltip';

interface Props extends React.HtmlHTMLAttributes<HTMLMenuElement> {
  children?: React.ReactNode;
  className?: string;
  innerRef?: React.Ref<HTMLUListElement>;
  maxHeight?: string;
  size?: InputSizeKeys;
}

export function DropdownMenu({
  children,
  className,
  innerRef,
  maxHeight = 'inherit',
  size = 'small',
  ...menuProps
}: Props) {
  return (
    <DropdownMenuWrapper
      className={classNames('dropdown-menu', className)}
      ref={innerRef}
      role="menu"
      style={{ '--inputSize': INPUT_SIZES[size], maxHeight }}
      {...menuProps}
    >
      {children}
    </DropdownMenuWrapper>
  );
}

interface ListItemProps {
  children?: React.ReactNode;
  className?: string;
  innerRef?: React.Ref<HTMLLIElement>;
  onFocus?: VoidFunction;
  onPointerEnter?: VoidFunction;
  onPointerLeave?: VoidFunction;
}

type ItemLinkProps = Omit<ListItemProps, 'innerRef'> &
  Pick<LinkProps, 'disabled' | 'icon' | 'onClick' | 'to'> & {
    innerRef?: React.Ref<HTMLAnchorElement>;
  };

export function ItemLink(props: ItemLinkProps) {
  const { children, className, disabled, icon, onClick, innerRef, to, ...liProps } = props;
  return (
    <li {...liProps}>
      <ItemLinkStyled
        className={classNames(className, { disabled })}
        disabled={disabled}
        icon={icon}
        onClick={onClick}
        ref={innerRef}
        role="menuitem"
        showExternalIcon={false}
        to={to}
      >
        {children}
      </ItemLinkStyled>
    </li>
  );
}

interface ItemNavLinkProps extends ItemLinkProps {
  end?: boolean;
}

export function ItemNavLink(props: ItemNavLinkProps) {
  const { children, className, disabled, end, icon, onClick, innerRef, to, ...liProps } = props;
  return (
    <li {...liProps}>
      <ItemNavLinkStyled
        className={classNames(className, { disabled })}
        disabled={disabled}
        end={end}
        onClick={onClick}
        ref={innerRef}
        role="menuitem"
        to={to}
      >
        {icon}
        {children}
      </ItemNavLinkStyled>
    </li>
  );
}

interface ItemButtonProps extends ListItemProps {
  disabled?: boolean;
  icon?: React.ReactNode;
  onClick: React.MouseEventHandler<HTMLButtonElement>;
}

export function ItemButton(props: ItemButtonProps) {
  const { children, className, disabled, icon, innerRef, onClick, ...liProps } = props;
  return (
    <li ref={innerRef} role="none" {...liProps}>
      <ItemButtonStyled className={className} disabled={disabled} onClick={onClick} role="menuitem">
        {icon}
        {children}
      </ItemButtonStyled>
    </li>
  );
}

export const ItemDangerButton = styled(ItemButton)`
  --color: ${themeContrast('dropdownMenuDanger')};
`;

interface ItemCheckboxProps extends ListItemProps {
  checked: boolean;
  disabled?: boolean;
  id?: string;
  onCheck: (checked: boolean, id?: string) => void;
}

export function ItemCheckbox(props: ItemCheckboxProps) {
  const { checked, children, className, disabled, id, innerRef, onCheck, onFocus, ...liProps } =
    props;
  return (
    <li ref={innerRef} role="none" {...liProps}>
      <ItemCheckboxStyled
        checked={checked}
        className={classNames(className, { disabled })}
        disabled={disabled}
        id={id}
        onCheck={onCheck}
        onFocus={onFocus}
      >
        {children}
      </ItemCheckboxStyled>
    </li>
  );
}

interface ItemRadioButtonProps extends ListItemProps {
  checked: boolean;
  disabled?: boolean;
  onCheck: (value: string) => void;
  value: string;
}

export function ItemRadioButton(props: ItemRadioButtonProps) {
  const { checked, children, className, disabled, innerRef, onCheck, value, ...liProps } = props;
  return (
    <li ref={innerRef} role="none" {...liProps}>
      <ItemRadioButtonStyled
        checked={checked}
        className={classNames(className, { disabled })}
        disabled={disabled}
        onCheck={onCheck}
        value={value}
      >
        {children}
      </ItemRadioButtonStyled>
    </li>
  );
}

interface ItemCopyProps {
  children?: React.ReactNode;
  className?: string;
  copyValue: string;
}

export function ItemCopy(props: ItemCopyProps) {
  const { children, className, copyValue } = props;
  return (
    <ClipboardBase>
      {({ setCopyButton, copySuccess }) => (
        <Tooltip overlay={translate('copied_action')} visible={copySuccess}>
          <li role="none">
            <ItemButtonStyled
              className={className}
              data-clipboard-text={copyValue}
              ref={setCopyButton}
              role="menuitem"
            >
              {children}
            </ItemButtonStyled>
          </li>
        </Tooltip>
      )}
    </ClipboardBase>
  );
}

interface ItemDownloadProps extends ListItemProps {
  download: string;
  href: string;
}

export function ItemDownload(props: ItemDownloadProps) {
  const { children, className, download, href, innerRef, ...liProps } = props;
  return (
    <li ref={innerRef} role="none" {...liProps}>
      <ItemDownloadStyled
        className={className}
        download={download}
        href={href}
        rel="noopener noreferrer"
        role="menuitem"
        target="_blank"
      >
        {children}
      </ItemDownloadStyled>
    </li>
  );
}

export const ItemHeaderHighlight = styled.span`
  color: ${themeContrast('searchHighlight')};
  font-weight: 600;
`;

export const ItemHeader = styled.li`
  background-color: ${themeColor('dropdownMenuHeader')};
  color: ${themeContrast('dropdownMenuHeader')};

  ${tw`sw-py-2 sw-px-3`}
`;
ItemHeader.defaultProps = { className: 'dropdown-menu-header', role: 'menuitem' };

export const ItemDivider = styled.li`
  height: 1px;
  background-color: ${themeColor('popupBorder')};

  ${tw`sw-my-1 sw--mx-2`}
  ${tw`sw-overflow-hidden`};
`;
ItemDivider.defaultProps = { role: 'separator' };

const DropdownMenuWrapper = styled.ul`
  background-color: ${themeColor('dropdownMenu')};
  color: ${themeContrast('dropdownMenu')};
  width: var(--inputSize);
  list-style: none;

  ${tw`sw-flex sw-flex-col`}
  ${tw`sw-box-border`};
  ${tw`sw-min-w-input-small`}
  ${tw`sw-py-2`}
  ${tw`sw-body-sm`}

  &:focus {
    outline: none;
  }
`;

const itemStyle = (props: ThemedProps) => css`
  color: var(--color);
  background-color: ${themeColor('dropdownMenu')(props)};
  border: none;
  border-bottom: none;
  text-decoration: none;
  transition: none;

  ${tw`sw-flex sw-items-center`}
  ${tw`sw-body-sm`}
  ${tw`sw-box-border`}
  ${tw`sw-w-full`}
  ${tw`sw-text-left`}
  ${tw`sw-py-2 sw-px-3`}
  ${tw`sw-truncate`};
  ${tw`sw-cursor-pointer`}

  &.active,
  &:active,
  &.active:active,
  &:hover,
  &.active:hover {
    color: var(--color);
    background-color: ${themeColor('dropdownMenuHover')(props)};
    text-decoration: none;
    outline: none;
    border: none;
    border-bottom: none;
  }

  &:focus,
  &:focus-within,
  &.active:focus,
  &.active:focus-within {
    color: var(--color);
    background-color: ${themeColor('dropdownMenuFocus')(props)};
    text-decoration: none;
    outline: ${themeBorder('focus', 'dropdownMenuFocusBorder')(props)};
    outline-offset: -4px;
    border: none;
    border-bottom: none;
  }

  &:disabled,
  &.disabled {
    color: ${themeContrast('dropdownMenuDisabled')(props)};
    background-color: ${themeColor('dropdownMenuDisabled')(props)};
    pointer-events: none !important;

    ${tw`sw-cursor-not-allowed`};
  }

  & > svg {
    ${tw`sw-mr-2`}
  }
`;

const ItemNavLinkStyled = styled(NavLink)`
  --color: ${themeContrast('dropdownMenu')};
  ${itemStyle};
`;

const ItemLinkStyled = styled(BaseLink)`
  --color: ${themeContrast('dropdownMenu')};
  ${itemStyle}
`;

const ItemButtonStyled = styled.button`
  --color: ${themeContrast('dropdownMenu')};
  ${itemStyle}
`;

const ItemDownloadStyled = styled.a`
  --color: ${themeContrast('dropdownMenu')};
  ${itemStyle}
`;

const ItemCheckboxStyled = styled(Checkbox)`
  --color: ${themeContrast('dropdownMenu')};
  ${itemStyle}
`;

const ItemRadioButtonStyled = styled(RadioButton)`
  --color: ${themeContrast('dropdownMenu')};
  ${itemStyle}
`;
