import { Html } from "@kitajs/html";

export default function (): JSX.Element {
  return (
    <div class="sidebar">
      <input id="sidebar" type="checkbox" class="sidebar-toggle" />
      <div class="sidebar-menu">
        <ul class="menu bg-base-200 text-base-content min-h-full w-full p-4">
          <li>
            <a>Item 1</a>
          </li>
          <li>
            <a>Item 2</a>
          </li>
        </ul>
      </div>
      <label for="sidebar" aria-label="close sidebar" class="sidebar-overlay" />
      <div class="sidebar-content">
        <label class="btn btn-square btn-ghost sidebar-button" for="sidebar">
          <span class="icon-[lucide--menu] w-5 h-5" />
        </label>
      </div>
    </div>
  );
}
