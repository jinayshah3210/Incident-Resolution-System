import React from 'react';

/** Catches rendering errors so a bug shows a recoverable message instead of a white screen. */
export default class ErrorBoundary extends React.Component {
  constructor(props) { super(props); this.state = { failed: false }; }
  static getDerivedStateFromError() { return { failed: true }; }
  componentDidCatch(error, info) { console.error('ResolveIT UI error', error, info); }
  render() {
    if (!this.state.failed) return this.props.children;
    return <div className="fatal-error"><h2>Something went wrong on this page.</h2>
      <p>Your data is safe. Reload the page or return to your dashboard.</p>
      <button className="btn primary" onClick={() => { this.setState({ failed: false }); window.location.href = '/'; }}>Return to dashboard</button></div>;
  }
}
