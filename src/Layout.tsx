import { Html, PropsWithChildren } from "@kitajs/html";

export default function (
  props: PropsWithChildren<{ name?: string }>,
): JSX.Element {
  return (
    <>
      {`<!docstyle html>`}
      <html lang="en">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <title>{props.name}</title>
          <link rel="stylesheet" href="stylesheet.css" />
        </head>
        <body>
          {props.children}
          <script src="index.js" />
          <div></div>
        </body>
      </html>
    </>
  );
}
