import { PropsWithChildren } from "@kitajs/html";

type Props = { name?: string; css?: boolean; js?: boolean };

export default function (props: PropsWithChildren<Props>): JSX.Element {
  return (
    <>
      {`<!docstyle html>`}
      <html lang="en">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <title safe>{props.name}</title>
          {props.css && <link rel="stylesheet" href="stylesheet.css" />}
        </head>
        <body>
          {props.children}
          {props.js && <script src="script.js" />}
          <div></div>
        </body>
      </html>
    </>
  );
}
