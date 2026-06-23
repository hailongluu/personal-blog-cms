import { Helmet } from 'react-helmet-async';

export default function AboutPage() {
  return (
    <>
      <Helmet>
        <title>About — Personal Blog</title>
        <meta name="description" content="Learn more about me — my background, interests, and what this blog is about." />
      </Helmet>

      <div className="max-w-3xl mx-auto px-4 py-12 md:py-16">
        {/* Hero */}
        <div className="mb-12 text-center md:text-left">
          <div className="w-24 h-24 rounded-full bg-stone-200 mx-auto md:mx-0 mb-6 flex items-center justify-center text-4xl">
            👋
          </div>
          <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">Hi, I'm [Your Name]</h1>
          <p className="text-lg text-stone-500 max-w-xl">
            A software developer passionate about building things for the web.
          </p>
        </div>

        {/* Content */}
        <div className="prose prose-stone prose-lg max-w-none space-y-6">
          <p>
            I'm a full-stack developer with a love for clean code, thoughtful design,
            and open source. I spend my days building web applications and my nights
            exploring new technologies, writing, and tinkering with side projects.
          </p>

          <h2>What I do</h2>
          <p>
            I specialize in modern web development with TypeScript, React, and Node.js.
            I'm interested in developer tooling, design systems, and building products
            that make people's lives easier.
          </p>

          <h2>This blog</h2>
          <p>
            This is my corner of the internet where I share what I'm learning, projects
            I'm building, and thoughts on technology and life. I write about software
            development, productivity, and occasionally other topics that catch my interest.
          </p>

          <h2>Get in touch</h2>
          <p>
            I'm always happy to connect with fellow developers and interesting people.
            Feel free to reach out via{' '}
            <a href="https://twitter.com" className="text-stone-900 underline">
              Twitter
            </a>{' '}
            or{' '}
            <a href="https://github.com" className="text-stone-900 underline">
              GitHub
            </a>
            . For longer conversations, email me at hello@example.com.
          </p>
        </div>
      </div>
    </>
  );
}
