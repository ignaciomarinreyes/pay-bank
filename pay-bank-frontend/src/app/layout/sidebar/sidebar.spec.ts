import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SidebarComponent } from './sidebar';

describe('SidebarComponent', () => {
  it('has a "Movimientos" link to /movements', async () => {
    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(SidebarComponent);
    await fixture.whenStable();

    const link = (fixture.nativeElement as HTMLElement).querySelector('a')!;
    expect(link.textContent?.trim()).toBe('Movimientos');
    expect(link.getAttribute('href')).toBe('/movements');
  });
});
