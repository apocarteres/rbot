import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from '../../../shared/app-config';
import { App } from './app/app';
import { routes } from './app/app.routes';

bootstrapApplication(App, appConfig(routes)).catch((failure: unknown) => console.error(failure));
