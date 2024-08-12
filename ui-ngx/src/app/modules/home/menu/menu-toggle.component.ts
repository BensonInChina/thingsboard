///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionPreferencesUpdateOpenedMenuSection } from '@core/auth/auth.actions';

@Component({
  selector: 'tb-menu-toggle',
  templateUrl: './menu-toggle.component.html',
  styleUrls: ['./menu-toggle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MenuToggleComponent implements OnInit {

  @Input() section: MenuSection;

  constructor(private router: Router,
              private store: Store<AppState>) {
  }

  ngOnInit() {
  }

  sectionHeight(): string {
    let height = 0;

    // Add height for primary menu section if it's opened
    if (this.section.opened) {
      height += this.section.pages.length * 40;

      // Calculate height for secondary menus if any are opened
      this.section.pages.forEach(page => {
        if (page.opened) {
          height += (page.pages?.length || 0) * 40;
        }
      });
    }

    return height + 'px';
  }

  toggleSection(event: MouseEvent) {
    event.stopPropagation();
    this.section.opened = !this.section.opened;
    if (!this.section.opened) {
      this.section.pages.forEach(page => {
        if (page.type === 'toggle') {
          page.opened = false;
        }
      });
    }
    this.store.dispatch(new ActionPreferencesUpdateOpenedMenuSection({path: this.section.path, opened: this.section.opened}));
  }

  trackBySectionPages(index: number, section: MenuSection){
    return section.id;
  }
}
