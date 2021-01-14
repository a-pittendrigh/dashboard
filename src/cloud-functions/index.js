const Firestore = require('@google-cloud/firestore');
const PROJECTID = 'test';
const COLLECTION_NAME = 'cloud-functions-firestore';
const firestore = new Firestore({
  projectId: PROJECTID,
  timestampsInSnapshots: true,
});

exports.main = (req, res) => {
  if (req.method === 'DELETE') throw 'not yet built';
  if (req.method === 'POST') {
    // store/insert a new document
    const data = (req.body) || {};
    const ttl = Number.parseInt(data.ttl);
    const ciphertext = (data.ciphertext || '').replace(/[^a-zA-Z0-9\-]*/g, '');
    const created = new Date().getTime();
    return firestore.collection(COLLECTION_NAME)
      .add({ created, ttl, ciphertext })
      .then(doc => {
        return res.status(200).send(doc);
      }).catch(err => {
        console.error(err);
        return res.status(404).send({ error: 'unable to store', err });
      });
  }
  // read/retrieve an existing document by id
  if (!(req.query && req.query.id)) {
    return res.status(404).send({ error: 'No Id' });
  }
  const id = req.query.id.replace(/[^a-zA-Z0-9]/g, '').trim();
  if (!(id && id.length)) {
    return res.status(404).send({ error: 'Empty Id' });
  }
  return firestore.collection(COLLECTION_NAME)
    .doc(id)
    .get()
    .then(doc => {
      if (!(doc && doc.exists)) {
        return res.status(404).send({ error: 'Unable to find the document' });
      }
      const data = doc.data();
      return res.status(200).send(data);
    }).catch(err => {
      console.error(err);
      return res.status(404).send({ error: 'Unable to retrieve the document' });
    });
};
