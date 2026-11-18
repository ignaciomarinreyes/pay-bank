import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginatorComponent } from './paginator';

describe('PaginatorComponent', () => {
  let fixture: ComponentFixture<PaginatorComponent>;
  let el: HTMLElement;
  let emitted: number[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PaginatorComponent] }).compileComponents();
    fixture = TestBed.createComponent(PaginatorComponent);
    el = fixture.nativeElement as HTMLElement;
    emitted = [];
    fixture.componentInstance.pageChange.subscribe((p) => emitted.push(p));
  });

  async function render(page: number, totalPages: number): Promise<void> {
    fixture.componentRef.setInput('page', page);
    fixture.componentRef.setInput('totalPages', totalPages);
    await fixture.whenStable();
  }

  const button = (label: string) =>
    Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(label))!;

  it('shows the current page and disables "Anterior" on the first page', async () => {
    await render(1, 3);

    expect(el.textContent).toContain('Página 1 de 3');
    expect(button('Anterior').disabled).toBe(true);
    expect(button('Siguiente').disabled).toBe(false);
  });

  it('disables "Siguiente" on the last page', async () => {
    await render(3, 3);

    expect(el.textContent).toContain('Página 3 de 3');
    expect(button('Siguiente').disabled).toBe(true);
    expect(button('Anterior').disabled).toBe(false);
  });

  it('emits the previous and next pages', async () => {
    await render(2, 3);

    button('Anterior').click();
    button('Siguiente').click();

    expect(emitted).toEqual([1, 3]);
  });
});
