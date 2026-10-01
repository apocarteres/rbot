import { Injectable } from '@angular/core';
import { AppClock } from './clock';

// REQ-TYPESCRIPT-CLOCK-003
@Injectable()
export class SystemClock extends AppClock {
  instant(): number {
    return Date.now();
  }
}
