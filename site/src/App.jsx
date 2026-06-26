import {
  HERO,
  ORIENTATION,
  PIPELINE,
  CHOICES,
  APPS,
  REEL,
  TIPS,
  REPO_URL,
  CYCLE_IG_URL,
  CYCLE_VIEWER_URL,
} from './data.js';

function Header() {
  return (
    <header className="site-header">
      <a className="brand" href="#top">
        <span className="brand-dot" aria-hidden="true" />
        Implementing the Cycle&nbsp;IG
      </a>
      <nav className="nav">
        <a href="#orientation">What&apos;s what</a>
        <a href="#technique">How it works</a>
        <a href="#choices">Choices</a>
        <a href="#apps">The apps</a>
        <a href="#tips">What we learned</a>
        <a className="nav-cta" href={REPO_URL} target="_blank" rel="noreferrer">
          GitHub ↗
        </a>
      </nav>
    </header>
  );
}

function Hero() {
  return (
    <section className="hero" id="top">
      <p className="eyebrow">{HERO.eyebrow}</p>
      <h1>{HERO.title}</h1>
      <p className="lede">{HERO.lede}</p>
      <div className="hero-actions">
        <a className="btn btn-primary" href={HERO.primary.href} target="_blank" rel="noreferrer">
          {HERO.primary.label}
        </a>
        <a className="btn btn-ghost" href={HERO.secondary.href} target="_blank" rel="noreferrer">
          {HERO.secondary.label}
        </a>
      </div>
    </section>
  );
}

function Reel() {
  return (
    <section className="reel" id="reel">
      <div className="section-head">
        <h2>See all four together</h2>
        <span className="badge">{REEL.duration}</span>
      </div>
      <p className="section-sub">{REEL.caption}</p>
      <div className="reel-frame">
        <video controls preload="metadata" playsInline src={REEL.video} />
      </div>
    </section>
  );
}

function Orientation() {
  return (
    <section className="orientation" id="orientation">
      <div className="section-head">
        <h2>How the pieces fit together</h2>
      </div>
      <p className="section-sub">
        This work involves three separate pieces: the specification the apps implement, the viewer a
        shared link opens in, and this repository of implementation work. Here is what each one is.
      </p>
      <div className="choice-grid">
        {ORIENTATION.map((item) => (
          <article className="choice-card" key={item.heading}>
            <h3>{item.heading}</h3>
            <p>{item.body}</p>
            <a className="orient-link" href={item.link.href} target="_blank" rel="noreferrer">
              {item.link.label} ↗
            </a>
          </article>
        ))}
      </div>
    </section>
  );
}

function Technique() {
  return (
    <section className="technique" id="technique">
      <div className="section-head">
        <h2>What every implementation does</h2>
      </div>
      <p className="section-sub">
        Each app implements the same SMART Link pipeline. The app encrypts the cycle data on the
        device, uploads only the ciphertext to a host, and shares a link. The decryption key stays in
        the link&apos;s fragment, so the host never receives it, and the clinician viewer fetches the
        ciphertext and decrypts it in the browser.
      </p>
      <ol className="pipeline">
        {PIPELINE.map((step, i) => (
          <li key={step.title}>
            <span className="pipeline-num">{String(i + 1).padStart(2, '0')}</span>
            <div>
              <h3>{step.title}</h3>
              <p>{step.body}</p>
            </div>
          </li>
        ))}
      </ol>
      <p className="technique-foot">
        The receiver path always lands in{' '}
        <a href={CYCLE_VIEWER_URL} target="_blank" rel="noreferrer">
          cycle.fhir.me/view
        </a>
        , which decrypts locally before rendering the clinical review.
      </p>
    </section>
  );
}

function Choices() {
  return (
    <section className="choices" id="choices">
      <div className="section-head">
        <h2>Shared implementation choices</h2>
      </div>
      <p className="section-sub">
        A few choices recur across every app, and each one keeps the change small and easy to review.
        They follow the data model in the IG: the share is a list of facts the user actually recorded.
        A logged day becomes a fact, an untouched day stays silent, and nothing is inferred. The IG
        organizes those facts in layers, with the core menstrual-bleeding yes/no underneath and richer
        detail like flow, temperature, and symptoms on top.
      </p>
      <div className="choice-grid">
        {CHOICES.map((c) => (
          <article className="choice-card" key={c.heading}>
            <h3>{c.heading}</h3>
            <p>{c.body}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

function AppCard({ app }) {
  return (
    <article className="app-card" id={app.id} style={{ '--accent': app.accent }}>
      <div className="app-media">
        <video controls preload="none" playsInline poster={app.poster} src={app.video} />
      </div>
      <div className="app-body">
        <div className="app-title">
          <h3>{app.name}</h3>
          <span className="app-platform">{app.platform}</span>
          <span className="badge">{app.duration}</span>
        </div>
        <p className="app-summary">{app.summary}</p>

        <h4 className="app-label">Implementation choices</h4>
        <ul className="app-highlights">
          {app.highlights.map((h) => (
            <li key={h}>{h}</li>
          ))}
        </ul>

        <h4 className="app-label">Key moments</h4>
        <ul className="moments">
          {app.moments.map(([t, label]) => (
            <li key={t}>
              <span className="moment-time">{t}</span>
              {label}
            </li>
          ))}
        </ul>

        <div className="shot-row">
          {app.shots.map((s) => (
            <a
              className="shot"
              key={s}
              href={`screenshots/${app.id}/${s}`}
              target="_blank"
              rel="noreferrer"
            >
              <img loading="lazy" src={`screenshots/${app.id}/${s}`} alt={`${app.name} ${s}`} />
            </a>
          ))}
        </div>

        <div className="app-links">
          {app.links.map((l) => (
            <a key={l.href} href={l.href} target="_blank" rel="noreferrer">
              {l.label} ↗
            </a>
          ))}
        </div>
      </div>
    </article>
  );
}

function Apps() {
  return (
    <section className="apps" id="apps">
      <div className="section-head">
        <h2>Four first forays</h2>
      </div>
      <p className="section-sub">
        Each demo starts with preloaded sample data shown in the app&apos;s normal views, then walks
        through scope, the live QR, the clinician viewer, and revoke.
      </p>
      <div className="app-list">
        {APPS.map((app) => (
          <AppCard key={app.id} app={app} />
        ))}
      </div>
    </section>
  );
}

function Tips() {
  return (
    <section className="tips" id="tips">
      <div className="section-head">
        <h2>What we learned</h2>
      </div>
      <p className="section-sub">
        These are the habits that kept each implementation small and safe. They carry over to any app
        adding Cycle IG support.
      </p>
      <ul className="tip-list">
        {TIPS.map((tip, i) => (
          <li key={tip}>
            <span className="tip-num">{i + 1}</span>
            {tip}
          </li>
        ))}
      </ul>
    </section>
  );
}

function Footer() {
  return (
    <footer className="site-footer">
      <p>
        This site documents how the Cycle FHIR IG — the sharing specification at{' '}
        <a href={CYCLE_IG_URL} target="_blank" rel="noreferrer">
          cycle.fhir.me
        </a>{' '}
        — was added to four open source period trackers. The implementation code, branches, and
        writeups live in the{' '}
        <a href={REPO_URL} target="_blank" rel="noreferrer">
          periodicity-impl
        </a>{' '}
        repository.
      </p>
      <div className="footer-links">
        <a href={CYCLE_IG_URL} target="_blank" rel="noreferrer">
          Cycle IG spec (cycle.fhir.me)
        </a>
        <a href={CYCLE_VIEWER_URL} target="_blank" rel="noreferrer">
          Viewer (cycle.fhir.me/view)
        </a>
        <a href={REPO_URL} target="_blank" rel="noreferrer">
          Implementation code (GitHub)
        </a>
      </div>
    </footer>
  );
}

export default function App() {
  return (
    <div className="page">
      <div className="aurora" aria-hidden="true" />
      <Header />
      <main>
        <Hero />
        <Orientation />
        <Technique />
        <Choices />
        <Apps />
        <Reel />
        <Tips />
      </main>
      <Footer />
    </div>
  );
}
