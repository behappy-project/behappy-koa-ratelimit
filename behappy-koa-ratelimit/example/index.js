import Koa from 'koa';
import rateLimit from '../dist/core.cjs';
import {koaBody} from 'koa-body';


const app = new Koa();
// body parser
app.use(koaBody());
// body parser
app.use(rateLimit({
  appName: 'demo',
  logEnable: true,
}));

// simulate router
app.use(async (ctx, next) => {
  await next();
  ctx.body = {
    code: 0,
    data: ['success'],
  };
});

const port = Number(process.env.PORT || 4000);
app.listen(port, '0.0.0.0')
  .on('listening', () => {
    /* eslint-disable no-console */
    console.log(`Listening on port: ${port}`);
    /* eslint-disable no-console */
  });
