import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Multiplatform',
    Svg: require('@site/static/img/java-icon.svg').default,
    description: (
      <>
        JSignPdf is written in Java and can be run on Windows, Linux, Mac OS and other OS-es where Java is available.
      </>
    ),
  },
  {
    title: 'Document preview',
    Svg: require('@site/static/img/preview.svg').default,
    description: (
      <>
        JSignPdf supports placing visible signatures by selecting area in document preview mode..
      </>
    ),
  },
  {
    title: 'Batch mode',
    Svg: require('@site/static/img/terminal.svg').default,
    description: (
      <>
        Even if you like more a command-line, JSignPdf is also for you. It can be fully controlled by
        command-line arguments, and it can work in a batch mode.
      </>
    ),
  },
  {
    title: 'Timestamps',
    Svg: require('@site/static/img/alarm-clock.svg').default,
    description: (
      <>
        Prove the time of signing. Request a timestamp from your favorite TSA.
      </>
    ),
  },
];

function Feature({Svg, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
