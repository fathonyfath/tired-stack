import { Suspense } from "@kitajs/html/suspense";
import { setTimeout } from "node:timers/promises";

async function getData(): Promise<string> {
  await setTimeout(5000);
  return "hello!";
}

async function AsyncComponent(): Promise<string> {
  const data = await getData();
  return <div>We have data from async getData: {data}</div>;
}

export default function (rid: number | string): JSX.Element {
  return (
    <div>
      <Suspense
        rid={rid}
        fallback={<div>Loading...</div>}
        catch={(err: any) => <div>Error: {err.stack}</div>}
      >
        <AsyncComponent />
      </Suspense>
    </div>
  );
}
