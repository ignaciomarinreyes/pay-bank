import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-title',
  templateUrl: './page-title.html',
  styleUrl: './page-title.scss',
})
export class PageTitleComponent {
  readonly title = input.required<string>();
}
