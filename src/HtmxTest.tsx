export default function (): JSX.Element {
  return (
    <div>
      <h1>HTMX Tester</h1>
      <p>
        Try opening <code>/htmx</code> to see the handler if it is accessed
        through browser.
      </p>
      <a href="/htmx">
        <b>[click here]</b>
      </a>
      <p>
        Now to use the HTMX mechanism, click the button below to update the
        content
      </p>
      <a href="/htmx" hx-get="/htmx" hx-trigger="click" hx-target="#htmx" hx-swap="afterend">
        <b>[HTMX-Boosted Click Here]</b>
      </a>
      <div id="htmx" style="padding: 8px; border: solid; margin-top: 8px;">
        Will replace this!
      </div>
    </div>
  );
}
