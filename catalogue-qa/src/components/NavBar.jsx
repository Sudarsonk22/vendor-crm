import React from "react";
import { NavLink } from "react-router-dom";
import "./NavBar.css";

const NavBar = () => {
  return (
    <nav className="top-nav">
      <span className="top-nav-brand">Vendor CRM</span>

      <div className="top-nav-links">
        <NavLink
          to="/"
          end
          className={({ isActive }) =>
            isActive ? "top-link top-link-active" : "top-link"
          }
        >
          Catalogue
        </NavLink>
        <NavLink
          to="/inventory"
          className={({ isActive }) =>
            isActive ? "top-link top-link-active" : "top-link"
          }
        >
          Inventory
        </NavLink>
      </div>
    </nav>
  );
};

export default NavBar;