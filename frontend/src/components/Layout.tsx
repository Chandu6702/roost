import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Layout() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  return (
    <>
      <nav className="nav">
        <span className="brand">Roost</span>
        <NavLink to="/" end>
          Monitors
        </NavLink>
        <NavLink to="/incidents">Incidents</NavLink>
        <NavLink to="/status">Status page</NavLink>
        <span className="spacer" />
        <button
          className="ghost"
          onClick={() => {
            logout()
            navigate('/login')
          }}
        >
          Log out
        </button>
      </nav>
      <div className="container">
        <Outlet />
      </div>
    </>
  )
}
